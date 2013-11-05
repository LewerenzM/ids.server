package org.icatproject.ids.integration.one;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeFactory;

import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.ids.integration.BaseTest;
import org.icatproject.ids.integration.util.Setup;
import org.junit.BeforeClass;
import org.junit.Test;

public class PutTest extends BaseTest {

	private static long timestamp;

	@BeforeClass
	public static void setup() throws Exception {
		setup = new Setup("one.properties");
		icatsetup();
	}

	@Test
	public void putOneFileTest() throws Exception {

		Dataset icatDs = (Dataset) icat.get(sessionId, "Dataset", datasetIds.get(0));

		String uploadedLocation = new File(icatDs.getLocation(), "uploaded_file2_" + timestamp)
				.getPath();
		Path fileOnFastStorage = setup.getStorageDir().resolve(uploadedLocation);
		assertFalse(Files.exists(fileOnFastStorage));
		Long dfid = testingClient.put(sessionId, Files.newInputStream(newFileLocation),
				"uploaded_file2_" + timestamp, datasetIds.get(0), supportedDatafileFormat.getId(),
				"A rather splendid datafile", 201);

		Datafile df = (Datafile) icat.get(sessionId, "Datafile", dfid);
		assertEquals("A rather splendid datafile", df.getDescription());
		assertNull(df.getDoi());
		assertNull(df.getDatafileCreateTime());
		assertNull(df.getDatafileModTime());
		assertTrue(Files.exists(fileOnFastStorage));

		uploadedLocation = new File(icatDs.getLocation(), "uploaded_file3_" + timestamp).getPath();
		fileOnFastStorage = setup.getStorageDir().resolve(uploadedLocation);
		assertFalse(Files.exists(fileOnFastStorage));
		dfid = testingClient.put(sessionId, Files.newInputStream(newFileLocation),
				"uploaded_file3_" + timestamp, datasetIds.get(0), supportedDatafileFormat.getId(),
				"An even better datafile", "7.1.3", new Date(420000), new Date(42000), 201);
		df = (Datafile) icat.get(sessionId, "Datafile", dfid);
		assertEquals("An even better datafile", df.getDescription());
		assertEquals("7.1.3", df.getDoi());

		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		gregorianCalendar.setTime(new Date(420000));
		assertEquals(datatypeFactory.newXMLGregorianCalendar(gregorianCalendar),
				df.getDatafileCreateTime());
		gregorianCalendar.setTime(new Date(42000));
		assertEquals(datatypeFactory.newXMLGregorianCalendar(gregorianCalendar),
				df.getDatafileModTime());

		assertTrue(Files.exists(fileOnFastStorage));

	}
}
