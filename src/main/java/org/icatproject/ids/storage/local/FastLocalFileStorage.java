package org.icatproject.ids.storage.local;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ids.storage.StorageInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TConfig;
import de.schlichtherle.truezip.file.TFile;
import de.schlichtherle.truezip.fs.archive.zip.ZipDriver;
import de.schlichtherle.truezip.socket.sl.IOPoolLocator;

public class FastLocalFileStorage implements StorageInterface {
	
	private final static Logger logger = LoggerFactory.getLogger(FastLocalFileStorage.class);
	
	final int BUFSIZ = 2048;
	final String STORAGE_ZIP_DIR = "/home/wojtek/icat/icatzipdata/";
	final String STORAGE_DIR = "/home/wojtek/icat/icatdata/";
	final String STORAGE_PREPARED_DIR = "/home/wojtek/icat/icatprepareddata/";
	final LocalFileStorageCommons fsCommons = new LocalFileStorageCommons();
	
	public FastLocalFileStorage() {
		// enable detection of ZIP files as archives
		TConfig.get().setArchiveDetector(
		        new TArchiveDetector(
		            TArchiveDetector.NULL,
		            new Object[][] {
		                { "zip", new ZipDriver(IOPoolLocator.SINGLETON)},
		            }));
	}
	
	@Override
	public boolean datasetExists(Dataset dataset) throws Exception {
		return fsCommons.datasetExists(dataset, STORAGE_ZIP_DIR);
	}
	
	@Override
	public void getDataset(Dataset dataset, OutputStream os) throws Exception {
		fsCommons.getDataset(dataset, os, STORAGE_ZIP_DIR);
	}
	
	@Override
	public InputStream getDatasetInputStream(Dataset dataset) throws Exception {
		return fsCommons.getDatasetInputStream(dataset, STORAGE_ZIP_DIR);
	}
	
	@Override
	public void putDataset(Dataset dataset, InputStream is) throws Exception {
		fsCommons.putDataset(dataset, is, STORAGE_ZIP_DIR);
		
		// unzip the dataset
		File tempdir = File.createTempFile("tmp", null, new File(STORAGE_DIR));
		File dir = new File(STORAGE_DIR, dataset.getLocation());
		File archdir = new File(STORAGE_ZIP_DIR, dataset.getLocation());
		tempdir.delete();
		tempdir.mkdir();
		dir.getParentFile().mkdirs();
		unzip(new File(archdir, "files.zip"), tempdir);
		tempdir.renameTo(dir);		
	}
	
	@Override
	public void deleteDataset(Dataset dataset) throws Exception {
		fsCommons.deleteDataset(dataset, STORAGE_ZIP_DIR);
		for (Datafile df : dataset.getDatafiles()) {
			File explodedFile = new File(STORAGE_DIR, df.getLocation());
			explodedFile.delete();
		}
	}
	
//	@Override
//	public boolean datafileExists(Datafile datafile) throws Exception {
//		return 
//	}
	
	public long putDatafile(String location, InputStream is, Dataset dataset) throws Exception {
		File file = new File(STORAGE_DIR, location);
		File filesZip = new File(new File(STORAGE_ZIP_DIR, dataset.getLocation()), "files.zip");
		if (!filesZip.exists()) {
			logger.warn("Couldn't find zipped DS: " + filesZip.getAbsolutePath() + " in Fast.putDatafile");
			throw new FileNotFoundException(filesZip.getAbsolutePath());
		}
		fsCommons.writeInputStreamToFile(file, is);
		// TODO write new file to zip; what should be new file's location in zip?
//		String locationInZip = "new-file";
//		TFile fileInZip = new TFile(filesZip, locationInZip);
//		new TFile(file).cp_r(fileInZip);
		return file.length();
	};
	
	@Override
	public void prepareZipForRequest(java.util.Set<Datafile> datafiles, String zipName, boolean compress) throws Exception {
		logger.info(String.format("zipping %s datafiles", datafiles.size()));
        long startTime = System.currentTimeMillis();
        File zipFile = new File(STORAGE_PREPARED_DIR, zipName);
        writeZipFileFromDatafiles(zipFile, datafiles, 
        		STORAGE_DIR, compress);
        long endTime = System.currentTimeMillis();
        logger.info("Time took to zip the files: " + (endTime - startTime));
	}
	
	@Override
	public void getPreparedZip(String zipName, OutputStream os, long offset) throws Exception {
		fsCommons.getPreparedZip(zipName, os, offset, STORAGE_PREPARED_DIR);
	}
	
	private void unzip(File zip, File dir) throws IOException {
		final ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
		ZipEntry entry;
		while ((entry = zis.getNextEntry()) != null) {
			final String name = entry.getName();
			final File file = new File(dir, name);
			System.out.println("Found " + name);
			if (entry.isDirectory()) {
				file.mkdir();
			} else {
				int count;
				final byte data[] = new byte[BUFSIZ];
				final BufferedOutputStream dest = new BufferedOutputStream(new FileOutputStream(file), BUFSIZ);
				while ((count = zis.read(data, 0, BUFSIZ)) != -1) {
					dest.write(data, 0, count);
				}
				dest.close();
			}
		}
		zis.close();
	}
	
	public static void writeZipFileFromDatafiles(File zipFile, Set<Datafile> fileSet,
            String relativePath, boolean compress) {
        if (fileSet.isEmpty()) {
            // Create empty file
            try {
                zipFile.createNewFile();
            } catch (IOException ex) {
                logger.error("writeZipFileFromDatafiles", ex);
            }
            return;
        }

        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            // set whether to compress the zip file or not
            if (compress == true) {
                zos.setMethod(ZipOutputStream.DEFLATED);
            } else {
                // using compress with level 0 instead of archive (STORED) because
                // STORED requires you to set CRC, size and compressed size
                // TODO: find efficient way of calculating CRC
                zos.setMethod(ZipOutputStream.DEFLATED);
                zos.setLevel(0);
                //zos.setMethod(ZipOutputStream.STORED);
            }
            for (Datafile file : fileSet) {
            	logger.info("Adding file " + file.getLocation() + " to zip");
                addToZip(zipFile, file.getLocation(), zos, relativePath);
            }

            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addToZip(File directoryToZip, String fileStr, ZipOutputStream zos,
            String relativePath) {
        try {
            File file = new File(relativePath, fileStr);
            FileInputStream fis = new FileInputStream(file);
            // to the directory being zipped, so chop off the rest of the path
            String zipFilePath = file.getCanonicalPath().substring(relativePath.length(),
                    file.getCanonicalPath().length());
            if (zipFilePath.startsWith(File.separator)) {
                zipFilePath = zipFilePath.substring(1);
            }
            
            logger.info("Writing '" + zipFilePath + "' to zip file");
            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            try {
                zos.putNextEntry(zipEntry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
                zos.closeEntry();
                fis.close();
            } catch (ZipException ex) {
                logger.info("Skipping the file" + ex);
                fis.close();
            }
        } catch (IOException ex) {
            logger.error("addToZip", ex);
        }
    }

}