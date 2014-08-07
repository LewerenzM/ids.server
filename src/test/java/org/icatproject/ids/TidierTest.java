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
	public void testCleanDatasetCache() throws Exception {
		Path top = Files.createTempDirectory(null);

		Path inva = top.resolve("inva");
		Path invb = top.resolve("invb");
		Path invc = top.resolve("invc");
		Files.createDirectories(inva);
		Files.createDirectories(invb);
		Files.createDirectories(invc);
		Files.copy(file, inva.resolve("x"));
		Thread.sleep(1000);
		Files.copy(file, inva.resolve("y"));
		Thread.sleep(1000);
		Files.copy(file, invb.resolve("y"));
		Thread.sleep(1000);
		Files.copy(file, invb.resolve("y.tmp"));
		Thread.sleep(1000);
		Files.copy(file, invb.resolve("z"));
		Thread.sleep(1000);
		Files.copy(file, invc.resolve("z"));
		assertTrue(Files.exists(inva.resolve("x")));
		assertTrue(Files.exists(inva.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y.tmp")));
		assertTrue(Files.exists(invb.resolve("z")));
		assertTrue(Files.exists(invc.resolve("z")));
		Tidier.cleanDatasetCache(top, 12000);
		assertTrue(Files.exists(inva.resolve("x")));
		assertTrue(Files.exists(inva.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y.tmp")));
		assertTrue(Files.exists(invb.resolve("z")));
		assertTrue(Files.exists(invc.resolve("z")));
		Tidier.cleanDatasetCache(top, 5000);
		assertFalse(Files.exists(inva));
		assertFalse(Files.exists(invb.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y.tmp")));
		assertFalse(Files.exists(invb.resolve("z")));
		assertTrue(Files.exists(invc.resolve("z")));
		Tidier.cleanDatasetCache(top, 2000);
		assertFalse(Files.exists(inva));
		assertFalse(Files.exists(invb.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y.tmp")));
		assertFalse(Files.exists(invc.resolve("z")));
		Tidier.cleanDatasetCache(top, 1999);
		assertFalse(Files.exists(inva));
		assertFalse(Files.exists(invb.resolve("y")));
		assertTrue(Files.exists(invb.resolve("y.tmp")));
		assertFalse(Files.exists(invc.resolve("z")));
	}

	@Test
	public void testCleanPreparedDir() throws Exception {
		Path top = Files.createTempDirectory(null);

		Path pa = top.resolve("pa");
		Path pb = top.resolve("pb.tmp"); // File
		Path pc = top.resolve("tmp.pc"); // Dir
		Path pd = top.resolve("pd");
		Path pe = top.resolve("pe");
		Path pf = top.resolve("pf");

		Files.copy(file, pa);
		Thread.sleep(1000);
		Files.copy(file, pb);
		Thread.sleep(1000);
		Files.createDirectories(pc);
		Files.copy(file, pc.resolve("x"));
		Thread.sleep(1000);
		Files.createDirectories(pd);
		Files.copy(file, pd.resolve("y"));
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

		Tidier.cleanPreparedDir(top, 30000);
		assertTrue(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertTrue(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 15000);
		assertFalse(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertFalse(Files.exists(pd));
		assertTrue(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 12000);
		assertFalse(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertFalse(Files.exists(pd));
		assertFalse(Files.exists(pe));
		assertTrue(Files.exists(pf));
		Tidier.cleanPreparedDir(top, 1999);
		assertFalse(Files.exists(pa));
		assertTrue(Files.exists(pb));
		assertTrue(Files.exists(pc));
		assertFalse(Files.exists(pd));
		assertFalse(Files.exists(pe));
		assertFalse(Files.exists(pf));
	}

}
