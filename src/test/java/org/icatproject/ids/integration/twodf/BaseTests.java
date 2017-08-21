package org.icatproject.ids.integration.twodf;

import org.icatproject.ids.integration.BaseTest;
import org.icatproject.ids.integration.util.Setup;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class BaseTests extends BaseTest {

	@BeforeClass
	public static void setup() throws Exception {
		setup = new Setup("twodf.properties");
		icatsetup();
	}

	@Ignore("This test is very time consuming")
	@Test
	public void raceTest() throws Exception {
		super.raceTest();
	}

	@Test
	public void getIcatUrlTest() throws Exception {
		super.getIcatUrlTest();
	}

	@Test
	public void apiVersionTest() throws Exception {
		super.apiVersionTest();
	}

	@Test
	public void getDatafileIdsTest() throws Exception {
		super.getDatafileIdsTest();
	}

	@Test
	public void reliabilityTest() throws Exception {
		super.reliabilityTest();
	}

	@Test
	public void reliabilityTest2() throws Exception {
		super.reliabilityTest2();
	}

	@Test
	public void reliabilityTest3() throws Exception {
		super.reliabilityTest3();
	}

	@Test
	public void bigDataSelectionTest() throws Exception {
		super.bigDataSelectionTest();
	}

	@Test
	public void cloningTest() throws Exception {
		super.cloningTest();
	}

	@Test
	public void isPreparedTest() throws Exception {
		super.isPreparedTest();
	}

}