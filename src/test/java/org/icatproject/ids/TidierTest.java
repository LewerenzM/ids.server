package org.icatproject.ids;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.BeforeClass;
import org.junit.Test;

public class TidierTest {

	private static Path file;

	@BeforeClass
	static public void beforeClass() throws Exception {
		file = Files.createTempFile(null, null);
		Files.copy(new ByteArrayInputStream(new byte[2000]), file,
				StandardCopyOption.REPLACE_EXISTING);
	}

	@Test
	public void testCleanPreparedDir() throws Exception {
		Path top = Files.createTempDirectory(null);

		Path pa = top.resolve("pa");
		Path pb = top.resolve("pb");
		Path pc = top.resolve("pc");
		Path pd = top.resolve("pd");
		Path pe = top.resolve("pe");
		Path pf = top.resolve("pf");

		Files.copy(file, pa);
		Thread.sleep(1000);
		Files.copy(file, pb);
		Thread.sleep(1000);
		Files.copy(file, pc);
		Thread.sleep(1000);
		Files.copy(file, pd);
		Thread.sleep(1000);
		Files.copy(file, pe);
		Thread.sleep(1000);
		Files.copy(file, pf);
		Thread.sleep(1000);
		assertTrue(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertTrue(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));

		Tidier.cleanPreparedDir(top, 8);
		assertTrue(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertTrue(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 4);
		assertFalse(Files.exists(pa));
		assertFalse(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertTrue(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 2);
		assertFalse(Files.exists(pa));
		assertFalse(Files.exists(pb));
		assertFalse(Files.exists(pc));
		assertFalse(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 0);
		assertFalse(Files.exists(pa));
		assertFalse(Files.exists(pb));
		assertFalse(Files.exists(pc));
		assertFalse(Files.exists(pd));
		assertFalse(Files.exists(pe));
		assertFalse(Files.exists(pf));
	}

}
