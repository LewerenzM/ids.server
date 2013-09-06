package org.icatproject.ids.ids2;

import java.io.File;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ICAT;
import org.icatproject.ICATService;
import org.icatproject.ids.Setup;
import org.icatproject.ids.util.PropertyHandler;
import org.icatproject.idsclient.TestingClient;
import org.icatproject.idsclient.exception.TestingClientNotFoundException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class PutTest {

	private static Setup setup = null;
	private static ICAT icat;
	TestingClient testingClient;

	private static long timestamp;

	@BeforeClass
	public static void setup() throws Exception {
		setup = new Setup();
		final URL icatUrl = new URL(setup.getIcatUrl());
		final ICATService icatService = new ICATService(icatUrl, new QName("http://icatproject.org", "ICATService"));
		icat = icatService.getICATPort();
	}

	@Before
	public void clearFastStorage() throws Exception {
		File storageDir = new File(setup.getStorageDir());
		File storageZipDir = new File(setup.getStorageZipDir());
		FileUtils.deleteDirectory(storageDir);
		FileUtils.deleteDirectory(storageZipDir);
		storageDir.mkdir();
		storageZipDir.mkdir();
		testingClient = new TestingClient(setup.getIdsUrl());
		timestamp = System.currentTimeMillis();
	}
	
	@Test(expected = TestingClientNotFoundException.class)
	public void putToUnrestoredDataset() throws Exception {
		final int DS_NUM_FROM_PROPS = 0;
		File fileOnUsersDisk = new File(setup.getUserLocalDir(), "test_file.txt"); // this file will be uploaded
		String uploadedLocation = "./my_file_name.txt";
		testingClient.putTest(setup.getGoodSessionId(), "my_file_name.txt_" + timestamp, "xml", setup.getDatasetIds()
				.get(DS_NUM_FROM_PROPS), uploadedLocation, null, null, null, null, fileOnUsersDisk);
	}

	@Test
	public void putOneFileTest() throws Exception {
		final int DS_NUM_FROM_PROPS = 0;		
		Dataset icatDs = (Dataset) icat.get(setup.getGoodSessionId(), "Dataset",
				Long.parseLong(setup.getDatasetIds().get(DS_NUM_FROM_PROPS)));
		File fileOnUsersDisk = new File(setup.getUserLocalDir(), "test_file.txt"); // this file will be uploaded
		String uploadedLocation = new File(icatDs.getLocation(), "my_file_name.txt").getPath();
		File fileOnFastStorage = new File(setup.getStorageDir(), uploadedLocation);
		
		File dirOnFastStorage = new File(setup.getStorageDir(), icatDs.getLocation());
		File zipOnFastStorage = new File(new File(setup.getStorageZipDir(), icatDs.getLocation()), "files.zip");
		testingClient.restoreTest(setup.getGoodSessionId(), null, setup.getDatasetIds().get(DS_NUM_FROM_PROPS), null);
		int retryLimit = 5;
		do {
			Thread.sleep(1000);
		} while ((!dirOnFastStorage.exists() || !zipOnFastStorage.exists()) && retryLimit-- > 0);
		assertTrue("File " + dirOnFastStorage.getAbsolutePath() + " should have been restored, but doesn't exist",
				dirOnFastStorage.exists());
		assertTrue("Zip in " + zipOnFastStorage.getAbsolutePath() + " should have been restored, but doesn't exist",
				zipOnFastStorage.exists());
		
		testingClient.putTest(setup.getGoodSessionId(), "my_file_name.txt_" + timestamp, "xml", setup.getDatasetIds()
				.get(DS_NUM_FROM_PROPS), uploadedLocation, null, null, null, null, fileOnUsersDisk);
		retryLimit = 5;
		do {
			Thread.sleep(1000);
		} while (!fileOnFastStorage.exists() && retryLimit-- > 0);
		assertTrue("File " + fileOnFastStorage.getAbsolutePath() + " should have been created, but doesn't exist",
				fileOnFastStorage.exists());
	}

//	@Test
//	public void deleteOldZipOfDatasetTest() throws Exception {
//		final int DS_NUM_FROM_PROPS = 0;
//		Dataset icatDs = (Dataset) icat.get(setup.getGoodSessionId(), "Dataset",
//				Long.parseLong(setup.getDatasetIds().get(DS_NUM_FROM_PROPS)));
//		final File zipfile = new File(new File(setup.getStorageZipDir(), icatDs.getLocation()), "files.zip");
//		FileUtils.forceMkdir(zipfile.getParentFile());
//		zipfile.createNewFile();
//		assertTrue("File " + zipfile.getAbsolutePath() + " should have been created", zipfile.exists());
//
//		long timestamp = System.currentTimeMillis();
//		File fileOnSlowStorage = new File(setup.getUserLocalDir(), "test_file.txt");
//		testingClient.putTest(setup.getGoodSessionId(), "my_file_name.txt_" + timestamp, "xml", setup.getDatasetIds()
//				.get(DS_NUM_FROM_PROPS), null, null, null, null, null, fileOnSlowStorage);
//
//		assertFalse("File " + zipfile.getAbsolutePath() + " should have been removed", zipfile.exists());
//	}
}
