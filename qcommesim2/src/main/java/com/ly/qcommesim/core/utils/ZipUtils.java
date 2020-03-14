package com.ly.qcommesim.core.utils;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * author: LingYun
 * email: SWD-yun.ling@jrdcom.com
 * date: 2019/10/11 17:04
 * version: 1.0
 *
 * 文件或文件夹解压缩工具类
 */
public class ZipUtils {
    public static final String tag="zipUtils";
    public ZipUtils(){

    }

    /**
     * 解压zip到指定的路径
     * @param zipfilestring zip的名称
     * @param outpathstring 要解压缩路径
     * @throws Exception
     */
    public static void unzipFolder(String zipfilestring, String outpathstring) throws Exception {
        ZipInputStream inzip = new ZipInputStream(new FileInputStream(zipfilestring));
        ZipEntry zipentry;
        String szname = "";
        while ((zipentry = inzip.getNextEntry()) != null) {
            szname = zipentry.getName();
            if (zipentry.isDirectory()) {
                //获取部件的文件夹名
                szname = szname.substring(0, szname.length() - 1);
                File folder = new File(outpathstring + File.separator + szname);
                folder.mkdirs();
            } else {
//                Log.e(tag,outpathstring + File.separator + szname);
                File file = new File(outpathstring + File.separator + szname);
                if (!file.exists()){
                    Log.e(tag, "create the file:" + outpathstring + file.separator + szname);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                // 获取文件的输出流
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                // 读取（字节）字节到缓冲区
                while ((len = inzip.read(buffer)) != -1) {
                    // 从缓冲区（0）位置写入（字节）字节
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inzip.close();
    }

    public static void unzipfolder(String zipfilestring, String outpathstring, String szname) throws Exception {
        ZipInputStream inzip = new ZipInputStream(new FileInputStream(zipfilestring));
        ZipEntry zipentry;
        while ((zipentry = inzip.getNextEntry()) != null) {
            //szname = zipentry.getname();
            if (zipentry.isDirectory()) {
                //获取部件的文件夹名
                szname = szname.substring(0, szname.length() - 1);
                File folder = new File(outpathstring + File.separator + szname);
                folder.mkdirs();
            } else {
                Log.e(tag,outpathstring + File.separator + szname);
                File file = new File(outpathstring + File.separator + szname);
                if (!file.exists()){
                    Log.e(tag, "create the file:" + outpathstring + file.separator + szname);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                // 获取文件的输出流
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                // 读取（字节）字节到缓冲区
                while ((len = inzip.read(buffer)) != -1) {
                    // 从缓冲区（0）位置写入（字节）字节
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inzip.close();
    }

    /**
     * 压缩文件和文件夹
     * @param srcfilestring 要压缩的文件或文件夹
     * @param zipfilestring 解压完成的zip路径
     * @throws Exception
     */
    public static void zipfolder(String srcfilestring, String zipfilestring)throws Exception {
        //创建zip
        ZipOutputStream outzip = new ZipOutputStream(new FileOutputStream(zipfilestring));
        //创建文件
        File file = new File(srcfilestring);
        //压缩
        zipfiles(file.getParent()+file.separator, file.getName(), outzip);
        //完成和关闭
        outzip.finish();
        outzip.close();
    }

    /**
     * 压缩文件
     * @param folderstring
     * @param filestring
     * @param zipoutputsteam
     * @throws Exception
     */
    private static void zipfiles(String folderstring, String filestring, ZipOutputStream zipoutputsteam)throws Exception {
        if(zipoutputsteam == null)
            return;
        File file = new File(folderstring+filestring);
        if (file.isFile()) {
            ZipEntry zipentry = new ZipEntry(filestring);
            FileInputStream inputstream = new FileInputStream(file);
            zipoutputsteam.putNextEntry(zipentry);
            int len;
            byte[] buffer = new byte[4096];
            while((len=inputstream.read(buffer)) != -1)
            {
                zipoutputsteam.write(buffer, 0, len);
            }
            zipoutputsteam.closeEntry();
        }
        else {
            //文件夹
            String filelist[] = file.list();
            //没有子文件和压缩
            if (filelist.length <= 0) {
                ZipEntry zipentry = new ZipEntry(filestring+file.separator);
                zipoutputsteam.putNextEntry(zipentry);
                zipoutputsteam.closeEntry();
            }
            //子文件和递归
            for (int i = 0; i < filelist.length; i++) {
                zipfiles(folderstring, filestring+ file.separator+filelist[i], zipoutputsteam);
            }
        }
    }

    /**
     * 返回zip的文件输入流
     * @param zipfilestring zip的名称
     * @param filestring  zip的文件名
     * @return inputstream
     * @throws Exception
     */
    public static InputStream upzip(String zipfilestring, String filestring)throws Exception {
        ZipFile zipfile = new ZipFile(zipfilestring);
        ZipEntry zipentry = zipfile.getEntry(filestring);
        return zipfile.getInputStream(zipentry);
    }

    /**
     * 返回zip中的文件列表（文件和文件夹）
     * @param zipfilestring  zip的名称
     * @param bcontainfolder 是否包含文件夹
     * @param bcontainfile  是否包含文件
     * @return
     * @throws Exception
     */
    public static List<File> getFilelist(String zipfilestring, boolean bcontainfolder, boolean bcontainfile)throws Exception {
        List<File> filelist = new ArrayList<>();
        ZipInputStream inzip = new ZipInputStream(new FileInputStream(zipfilestring));
        ZipEntry zipentry;
        String szname = "";
        while ((zipentry = inzip.getNextEntry()) != null) {
            szname = zipentry.getName();
            if (zipentry.isDirectory()) {
                // 获取部件的文件夹名
                szname = szname.substring(0, szname.length() - 1);
                File folder = new File(szname);
                if (bcontainfolder) {
                    filelist.add(folder);
                }
            } else {
                File file = new File(szname);
                if (bcontainfile) {
                    filelist.add(file);
                }
            }
        }
        inzip.close();
        return filelist;
    }
}
