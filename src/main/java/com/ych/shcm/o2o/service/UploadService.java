package com.ych.shcm.o2o.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.UriParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import com.ych.core.model.SystemParameterKey;
import com.ych.core.service.SystemParameterService;
import com.ych.shcm.o2o.model.Constants;

/**
 * 上传文件业务对象
 *
 * @author U
 */
@Lazy
@Component("shcm.o2o.service.UploadService")
public class UploadService {

    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(UploadService.class);

    /**
     * 上传文件正式存放路径
     */
    @Value("${file.local.path}")
    private String uploadFolder;

    /**
     * 上传文件的临时目录
     */
    @Value("${file.local.temppath}")
    private String uploadTempFolder;

    /**
     * 服务器编号
     */
    @Value("${system.common.serverNo}")
    private String serverNo;

    /**
     * 上传目录对象
     */
    private FileObject folderObject;

    /**
     * 临时目录对象
     */
    private FileObject tempFolderObject;

    /**
     * 文件处理序号
     */
    private AtomicInteger fileSeq = new AtomicInteger();

    /**
     * 任务执行器
     */
    @Resource(name = Constants.TASK_EXECUTOR)
    private TaskExecutor taskExecutor;

    /**
     * 系统参数服务
     */
    @Autowired
    private SystemParameterService sysParamSvc;

    /**
     * 生成文件名<br>
     * 格式为yyyyMMdd/HHmmssSSS + 两位服务器编号 + 3位序号
     *
     * @param postFix
     *         文件后缀
     * @return 生成的文件名
     */
    private String generateFileName(String postFix) {
        long mills = System.currentTimeMillis();
        return DateFormatUtils.format(mills, "yyyyMMdd/HHmmssSSS") + StringUtils.leftPad(serverNo, 2, '0') + StringUtils.leftPad(String.valueOf(Math.abs(fileSeq.getAndIncrement() % 1000)), 3, '0') + "." + postFix;
    }

    /**
     * 生成指定文件名的保存路径<br>
     * 格式为yyyyMMdd/HHmmssSSS + 两位服务器编号 + 3位序号/文件名
     *
     * @param fileName
     *         文件名
     * @return 文件路径
     */
    private String generateFileNamePath(String fileName) {
        long mills = System.currentTimeMillis();
        return DateFormatUtils.format(mills, "yyyyMMdd/HHmmssSSS") + StringUtils.leftPad(serverNo, 2, '0') + StringUtils.leftPad(String.valueOf(Math.abs(fileSeq.getAndIncrement() % 1000)), 3, '0') + "/" + fileName;
    }

    /**
     * 获取文件的后缀名
     *
     * @param fileName
     *         文件名
     * @return 文件后缀名, 不包含.
     */
    private String getFilePostFix(String fileName) {
        int index = fileName.lastIndexOf(".");

        if (index <= 0) {
            return "";
        } else {
            return fileName.substring(index + 1);
        }
    }

    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        try {
            FileSystemManager fsManage = VFS.getManager();

            folderObject = fsManage.resolveFile(UriParser.encode(uploadFolder));
            tempFolderObject = fsManage.resolveFile(UriParser.encode(uploadTempFolder));

            if (!folderObject.exists()) {
                folderObject.createFolder();
            }

            if (!tempFolderObject.exists()) {
                tempFolderObject.createFolder();
            }
        } catch (FileSystemException e) {
            logger.error("Init FileObject failed", e);
        }
    }

    /**
     * 从文件路径中获取文件名
     *
     * @param filePath
     *         文件路径
     * @return 文件名
     */
    public String getFileName(String filePath) {
        int pathSpIdx = filePath.lastIndexOf('/');
        if (pathSpIdx < 0) {
            pathSpIdx = filePath.lastIndexOf('\\');
        }

        return pathSpIdx < 0 ? filePath : filePath.substring(pathSpIdx + 1);
    }

    /**
     * 拼装文件路径
     *
     * @param paths
     *         拼装的文件路径
     * @return 拼装后的文件路径
     */
    private String concatPath(String... paths) {
        StringBuffer buffer = new StringBuffer();

        for (String path : paths) {
            if (buffer.length() == 0) {
                buffer.append(path);
            } else {
                if (buffer.charAt(buffer.length() - 1) == '/') {
                    if (path.charAt(0) == '/') {
                        buffer.append(path.substring(1));
                    } else {
                        buffer.append(path);
                    }
                } else if (path.charAt(0) == '/') {
                    buffer.append(path);
                } else {
                    buffer.append('/').append(path);
                }
            }
        }

        return buffer.toString();
    }

    /**
     * 临时目录下文件对应的URL
     *
     * @param path
     *         子路径
     * @return 绝对路径, 如果path为null或全部为不可见字符则返回null
     */
    public String getFileTempUrl(String path) {
        String p = StringUtils.trimToNull(path);
        return p == null ? null : concatPath(sysParamSvc.getStringValue(SystemParameterKey.UPLOAD_URL_TEMP_PREFIX), path);
    }

    /**
     * 获取正式目录下文件对应的URL
     *
     * @param path
     *         子路径
     * @return 绝对路径, 如果path为null或全部为不可见字符则返回null
     */
    public String getFileUrl(String path) {
        String p = StringUtils.trimToNull(path);
        return p == null ? null : concatPath(sysParamSvc.getStringValue(SystemParameterKey.UPLOAD_URL_PREFIX), path);
    }

    /**
     * 从URL中将相对路径分离出来
     *
     * @param url
     *         URL
     * @return 返回的相对路径, 开头不为/
     * @throws IllegalArgumentException
     *         当传入的URL不是正式URL也不是临时URL时
     */
    public String seperateRelativePathFromUrl(String url) {
        String ret = StringUtils.trimToNull(url);
        if (ret == null) {
            return null;
        }

        String fileTempUrlPrefix = sysParamSvc.getStringValue(SystemParameterKey.UPLOAD_URL_TEMP_PREFIX);
        String fileUrlPrefix = sysParamSvc.getStringValue(SystemParameterKey.UPLOAD_URL_PREFIX);

        if (url.indexOf(fileTempUrlPrefix) == 0) {
            ret = url.substring(fileTempUrlPrefix.length());
        } else if (url.indexOf(fileUrlPrefix) == 0) {
            ret = url.substring(fileUrlPrefix.length());
        } else {
            throw new IllegalArgumentException("Illegal url, not include in formal and temp file server");
        }

        if (ret.charAt(0) == '/') {
            return ret.substring(1);
        } else {
            return ret;
        }
    }

    /**
     * 从URL中将相对路径分离出来
     *
     * @param url
     *         URL
     * @return 返回的相对路径, 开头为/
     * @throws IllegalArgumentException
     *         当传入的URL不是正式URL也不是临时URL时
     */
    public String seperateRelativePathFromUrlWithSlash(String url) {
        return addSlashIfNecessary(seperateRelativePathFromUrl(url));
    }

    /**
     * 将文件拷贝到正式目录
     *
     * @param file
     *         目录
     * @return 成功时返回true
     */
    public boolean copyFileToFormalFolder(String file) {
        if (StringUtils.isEmpty(file)) {
            return false;
        }

        file = removeSlashIfNecessary(file);

        FileObject tempFile = null;
        FileObject formalFile = null;

        InputStream is = null;
        OutputStream os = null;

        try {
            tempFile = VFS.getManager().resolveFile(tempFolderObject, UriParser.encode(file));

            if (!tempFile.exists()) {
                return false;
            }

            formalFile = VFS.getManager().resolveFile(folderObject, UriParser.encode(file));

            if (formalFile.exists()) {
                formalFile.delete();
            }

            formalFile.createFile();

            is = tempFile.getContent().getInputStream();
            os = formalFile.getContent().getOutputStream();

            IOUtils.copy(is, os);
        } catch (Exception e) {
            logger.error("Copy file to formal folder failed", e);
            return false;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);

            if (tempFile != null) {
                try {
                    tempFile.close();
                } catch (FileSystemException e) {
                    logger.error("Close temp file failed", e);
                }
            }

            if (formalFile != null) {
                try {
                    formalFile.close();
                } catch (FileSystemException e) {
                    logger.error("Close formal file failed", e);
                }
            }
        }

        return true;
    }

    /**
     * 将文件拷贝到正式目录
     *
     * @param fileList
     *         要拷贝的文件列表
     * @param copied
     *         已经拷贝成功的列表,拷贝成功的文件会添加到这个List中
     * @return 成功时返回true
     */
    public boolean copyFileToFormalFolder(List<String> fileList, List<String> copied) {
        for (String src : fileList) {
            if (copyFileToFormalFolder(src)) {
                copied.add(src);
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * 将文件拷贝到正式目录
     *
     * @param url
     *         目录
     * @return 成功时返回相对路径, 以/开头,否则返回null.
     */
    public String copyFileToFormalFolderByUrl(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }

        String path = seperateRelativePathFromUrl(url);

        if (copyFileToFormalFolder(path)) {
            return addSlashIfNecessary(path);
        } else {
            return null;
        }
    }

    /**
     * 如果路径前方是/,则移除
     *
     * @param path
     *         路径
     * @return 处理后的路径
     */
    public String removeSlashIfNecessary(String path) {
        String p = StringUtils.trimToNull(path);

        if (p.charAt(0) == '/') {
            return p.substring(1);
        }

        return p;
    }

    /**
     * 在路径最前方加上/,如果已有了则不处理
     *
     * @param path
     *         路径
     * @return 处理后的路径
     */
    public String addSlashIfNecessary(String path) {
        String p = StringUtils.trimToNull(path);

        if (p == null) {
            return null;
        }

        if (p.charAt(0) == '/') {
            return p;
        }

        return "/" + p;
    }

    /**
     * 清理一天以前的文件
     *
     * @param folder
     *         目录
     * @param deleteTopFolder
     *         是否删除顶层目录
     */
    private void cleanDayAgo(FileObject folder, boolean deleteTopFolder) {
        FileObject[] children;
        try {
            children = folder.getChildren();
        } catch (FileSystemException e) {
            logger.error("Get file children failed", e);
            return;
        }

        long mill = System.currentTimeMillis();

        // 删除一天以前的文件
        for (FileObject child : children) {
            try {
                if (child.getType() == FileType.FOLDER) {
                    cleanDayAgo(child, true);
                } else if (child.getContent().getLastModifiedTime() + 86400000 <= mill) {
                    child.delete();
                }
            } catch (FileSystemException e) {
                logger.error("Delete child file failed", e);
            }
        }

        if (deleteTopFolder) {
            try {
                if (folder.getChildren().length == 0) {
                    folder.delete();
                }
            } catch (FileSystemException e) {
                logger.error("Delete folder failed", e);
            }
        }
    }

    /**
     * 清理临时目录,会清理一天以前的文件
     */
    public void cleanTempFolder() {
        cleanDayAgo(tempFolderObject, false);
    }

    /**
     * 通过URL删除正式目录的文件
     *
     * @param url
     *         正式文件的URL
     */
    public void deleteFormalFileByUrl(String url) {
        deleteFromalFile(seperateRelativePathFromUrl(url));
    }

    /**
     * 通过URL删除正式目录的文件
     *
     * @param paths
     *         正式文件的URL
     */
    public void deleteFormalFileByUrl(Collection<String> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }

        ArrayList<String> list = new ArrayList<>(paths.size());

        for (String path : paths) {
            list.add(seperateRelativePathFromUrl(path));
        }

        deleteFromalFile(list);
    }

    /**
     * 如果父目录为空则一次删除没有文件的祖先
     *
     * @param fileObject
     *         文件对象
     * @throws FileSystemException
     *         系统异常
     */
    private void deleteParentsIfEmpty(FileObject fileObject) throws FileSystemException {
        FileObject parent = fileObject.getParent();
        if (parent.getChildren().length == 0) {
            parent.delete();
            deleteParentsIfEmpty(parent);
        }
    }

    /**
     * 执行删除正式文件
     *
     * @param path
     *         文件路径
     */
    private void execDeleteFormalFile(String path) {
        if (StringUtils.isNotEmpty(path)) {
            try {
                FileObject fileObject = VFS.getManager().resolveFile(folderObject, UriParser.encode(removeSlashIfNecessary(path)));
                if (fileObject.exists()) {
                    fileObject.delete();
                }
                deleteParentsIfEmpty(fileObject);
            } catch (FileSystemException e) {
                logger.error("Delete formal file {} failed", path, e);
            }
        }
    }

    /**
     * 从正式文件路径删除文件
     *
     * @param path
     *         文件路径
     */
    public void deleteFromalFile(final String path) {
        if (StringUtils.isEmpty(path)) {
            return;
        }

        taskExecutor.execute(new Runnable() {

            @Override
            public void run() {
                execDeleteFormalFile(path);
            }
        });

    }

    /**
     * 从正式文件路径删除文件
     *
     * @param paths
     *         文件列表
     */
    public void deleteFromalFile(final Collection<String> paths) {
        if (CollectionUtils.isEmpty(paths)) {
            return;
        }

        taskExecutor.execute(new Runnable() {

            @Override
            public void run() {
                for (String path : paths) {
                    execDeleteFormalFile(path);
                }
            }
        });
    }

    /**
     * 将正式目录下的文件拷贝一份副本
     *
     * @param path
     *         文件路径
     * @return 新文件的路径, 以/开头,拷贝失败或者原文件不存在时返回null
     */
    public String copyFormalFile(String path) {
        if (StringUtils.isEmpty(path)) {
            return null;
        }

        path = removeSlashIfNecessary(path);

        int idx = path.indexOf('/');
        idx = path.indexOf('/', idx + 1);

        String relativePath = concatPath(path.substring(0, idx), generateFileName(getFilePostFix(path)));

        FileObject oldFile = null;
        FileObject newFile = null;

        InputStream is = null;
        OutputStream os = null;

        try {
            oldFile = VFS.getManager().resolveFile(folderObject, UriParser.encode(path));

            if (!oldFile.exists()) {
                return null;
            }

            newFile = VFS.getManager().resolveFile(folderObject, UriParser.encode(relativePath));

            if (newFile.exists()) {
                newFile.delete();
            }

            newFile.createFile();

            is = oldFile.getContent().getInputStream();
            os = newFile.getContent().getOutputStream();

            IOUtils.copy(is, os);
        } catch (Exception e) {
            logger.error("Copy file to formal folder failed", e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);

            if (oldFile != null) {
                try {
                    oldFile.close();
                } catch (FileSystemException e) {
                    logger.error("Close old file failed", e);
                }
            }

            if (newFile != null) {
                try {
                    newFile.close();
                } catch (FileSystemException e) {
                    logger.error("Close new file failed", e);
                }
            }
        }

        return addSlashIfNecessary(relativePath);
    }

    /**
     * 将文件保存到临时目录下的指定子目录
     *
     * @param pathName
     *         目录名称
     * @param fileName
     *         文件名称
     * @param is
     *         输入流
     * @return 成功时返回相对临时目录的路径, 格式为pathName/yyyyMMdd/HHmmssSSS + 两位服务器编号 + 3位序号 +
     * . + 文件扩展名
     */
    public String saveFileToTempPath(String pathName, String fileName, InputStream is) {
        String relativePath = concatPath(pathName, generateFileName(getFilePostFix(fileName)));

        OutputStream os = null;
        FileObject fileObject = null;

        try {
            fileObject = VFS.getManager().resolveFile(tempFolderObject, UriParser.encode(relativePath));

            if (fileObject.exists()) {
                fileObject.delete();
            }

            fileObject.createFile();

            os = fileObject.getContent().getOutputStream();

            IOUtils.copy(is, os);

            fileObject.close();

        } catch (Exception e) {
            logger.error("Write upload file to folder:{}, with path:{}", tempFolderObject, relativePath, e);
            return null;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);

            if (fileObject != null) {
                try {
                    fileObject.close();
                } catch (FileSystemException e) {
                    logger.error("Close fileObject failed, relative path:{}", relativePath, e);
                }
            }
        }

        return relativePath;
    }

    /**
     * 创建临时目录下的文件.成功时返回相对临时目录的路径,格式为pathName/yyyyMMdd/HHmmssSSS + 两位服务器编号 + 3位序号
     * + . + 文件扩展名
     *
     * @param pathName
     *         目录名称
     * @param fileName
     *         文件名称
     * @return 失败时返回null
     */
    public FilePathFileObject createTempPathFileObject(String pathName, String fileName) {
        String relativePath = concatPath(pathName, generateFileNamePath(fileName));

        FileObject fileObject = null;
        boolean result = false;

        try {
            fileObject = VFS.getManager().resolveFile(tempFolderObject, UriParser.encode(relativePath));

            if (fileObject.exists()) {
                fileObject.delete();
            }

            fileObject.createFile();

            result = true;

            FilePathFileObject ret = new FilePathFileObject();
            ret.setPath(relativePath);
            ret.setFileObject(fileObject);
            return ret;
        } catch (Exception e) {
            logger.error("Write upload file to folder:{}, with path:{}", tempFolderObject, relativePath, e);
            return null;
        } finally {
            if (!result && fileObject != null) {
                try {
                    fileObject.close();
                } catch (FileSystemException e) {
                    logger.error("Close fileObject failed, relative path:{}", relativePath, e);
                }
            }
        }
    }

    /**
     * 文件路径下保存的文件对象
     *
     * @author U
     */
    public static class FilePathFileObject {

        /**
         * 文件保存的相对路径
         */
        private String path;

        /**
         * 文件对象
         */
        private FileObject fileObject;

        /**
         * @return 文件保存的相对路径
         */
        public String getPath() {
            return path;
        }

        /**
         * @param path
         *         文件保存的相对路径
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * @return 文件对象
         */
        public FileObject getFileObject() {
            return fileObject;
        }

        /**
         * @param fileObject
         *         文件对象
         */
        public void setFileObject(FileObject fileObject) {
            this.fileObject = fileObject;
        }

    }

    /**
     * 将临时目录下的指定文件拷贝入输出流
     *
     * @param file
     *         要拷贝的文件
     * @param os
     *         输出流
     * @return 操作成功返回true
     */
    public boolean copyTempFile(String file, OutputStream os) {
        if (StringUtils.isEmpty(file)) {
            return false;
        }

        file = removeSlashIfNecessary(file);

        FileObject tempFile = null;

        InputStream is = null;

        try {
            tempFile = VFS.getManager().resolveFile(tempFolderObject, UriParser.encode(file));

            if (!tempFile.exists()) {
                return false;
            }

            is = tempFile.getContent().getInputStream();

            IOUtils.copy(is, os);
        } catch (Exception e) {
            logger.error("Copy file to formal folder failed", e);
            return false;
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(os);

            if (tempFile != null) {
                try {
                    tempFile.close();
                } catch (FileSystemException e) {
                    logger.error("Close temp file failed", e);
                }
            }
        }

        return true;
    }

}
