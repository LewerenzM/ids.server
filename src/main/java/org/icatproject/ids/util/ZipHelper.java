package org.icatproject.ids.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ids.storage.StorageInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipHelper {

	private final static Logger logger = LoggerFactory.getLogger(ZipHelper.class);

	public static InputStream zipDataset(Dataset dataset, boolean compress, StorageInterface storageInterface)
			throws IOException, FileNotFoundException {
		File tmpZipFile = new File(PropertyHandler.getInstance().getTmpDir(),
				new Long(System.currentTimeMillis()).toString() + ".zip");
		if (dataset.getDatafiles().isEmpty()) {
			// Create empty file
			tmpZipFile.createNewFile();
			return new ZipInputStream(new FileInputStream(tmpZipFile));
		}
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(tmpZipFile));

			// set whether to compress the zip file or not
			if (compress == true) {
				zos.setMethod(ZipOutputStream.DEFLATED);
			} else {
				// using compress with level 0 instead of archive (STORED)
				// because
				// STORED requires you to set CRC, size and compressed size
				// TODO: find efficient way of calculating CRC
				zos.setMethod(ZipOutputStream.DEFLATED);
				zos.setLevel(0);
			}
			for (Datafile df : dataset.getDatafiles()) {
				logger.info("Adding file " + df.getName() + " to zip");
				addToZip(df.getName(), zos, storageInterface.getDatafile(df));
			}
		} finally {
			if (zos != null) {
				try {
					zos.close();
				} catch (Exception e) {
					logger.warn("Couldn't close the stream to " + tmpZipFile.getAbsolutePath());
				}
			}
		}
		FileInputStream is = new FileInputStream(tmpZipFile);
//		ZipInputStream zis = new ZipInputStream(is);
		return is;
	}

	private static void addToZip(String pathInsideOfZip, ZipOutputStream zos, InputStream datafileIn) throws IOException {
			logger.info("Writing '" + pathInsideOfZip + "' to zip file");
			ZipEntry zipEntry = new ZipEntry(pathInsideOfZip);
			try {
				zos.putNextEntry(zipEntry);
				byte[] bytes = new byte[1024];
				int length;
				while ((length = datafileIn.read(bytes)) >= 0) {
					zos.write(bytes, 0, length);
				}
				zos.closeEntry();
				datafileIn.close();
			} catch (ZipException ex) {
				logger.info("Skipping the file" + ex);
				datafileIn.close();
			}
	}

}
