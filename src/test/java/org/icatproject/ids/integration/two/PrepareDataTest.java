package org.icatproject.ids.integration.two;

import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import org.icatproject.ids.integration.BaseTest;
import org.icatproject.ids.integration.util.Setup;
import org.icatproject.ids.integration.util.client.BadRequestException;
import org.icatproject.ids.integration.util.client.DataSelection;
import org.icatproject.ids.integration.util.client.InsufficientPrivilegesException;
import org.icatproject.ids.integration.util.client.TestingClient.Flag;

public class PrepareDataTest extends BaseTest {

    @BeforeClass
    public static void setup() throws Exception {
        setup = new Setup("two.properties");
        icatsetup();
    }

    @Test
    public void prepareArchivedDataset() throws Exception {
        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDataset(datasetIds.get(0)),
                Flag.NONE, 200);

        List<Long> ids = testingClient.getDatafileIds(preparedId, 200);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(datafileIds.get(0)));
        assertTrue(ids.contains(datafileIds.get(1)));

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(dirOnFastStorage);
    }

    @Test
    public void prepareTwoArchivedDatasets() throws Exception {

        Path dirOnFastStorage1 = getDirOnFastStorage(datasetIds.get(0));

        Path dirOnFastStorage2 = getDirOnFastStorage(datasetIds.get(1));
        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDataset(datasetIds.get(0))
                .addDataset(datasetIds.get(1)), Flag.NONE, 200);

        List<Long> ids = testingClient.getDatafileIds(preparedId, 200);
        assertEquals(4, ids.size());
        for (Long id : datafileIds) {
            assertTrue(ids.contains(id));
        }

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(dirOnFastStorage1);
        checkPresent(dirOnFastStorage2);
    }

    @Test
    public void prepareArchivedDatafile() throws Exception {
        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDatafile(datafileIds.get(0)),
                Flag.NONE, 200);

        List<Long> ids = testingClient.getDatafileIds(preparedId, 200);
        assertEquals(1, ids.size());
        assertTrue(ids.contains(datafileIds.get(0)));

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(dirOnFastStorage);
    }

    @Test
    public void prepareArchivedDatafileAndItsDataset() throws Exception {
        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDataset(datasetIds.get(0))
                .addDatafile(datafileIds.get(0)), Flag.NONE, 200);

        List<Long> ids = testingClient.getDatafileIds(preparedId, 200);
        assertEquals(2, ids.size());
        assertTrue(ids.contains(datafileIds.get(0)));
        assertTrue(ids.contains(datafileIds.get(1)));

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(dirOnFastStorage);
    }

    @Test(expected = BadRequestException.class)
    public void badSessionIdFormatTest() throws Exception {
        testingClient.prepareData("bad sessionId format", new DataSelection().addDataset(datasetIds.get(0)), Flag.NONE,
                400);
    }

    @Test
    public void noIdsTest() throws Exception {
        testingClient.prepareData(sessionId, new DataSelection(), Flag.NONE, 200);
    }

    @Test(expected = InsufficientPrivilegesException.class)
    public void nonExistingSessionIdTest() throws Exception {
        testingClient.prepareData("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                new DataSelection().addDataset(datasetIds.get(0)), Flag.NONE, 403);

    }

    @Test
    public void correctBehaviourTest() throws Exception {
        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDatafiles(datafileIds),
                Flag.NONE, 200);
        assertNotNull(preparedId);
    }

    @Test
    public void prepareRestoredDataset() throws Exception {
        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        testingClient.restore(sessionId, new DataSelection().addDataset(datasetIds.get(0)), 204);

        waitForIds();

        checkPresent(dirOnFastStorage);

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDataset(datasetIds.get(0)),
                Flag.NONE, 200);

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(dirOnFastStorage);
        checkPresent(setup.getPreparedCacheDir().resolve(preparedId));
    }

    @Test
    public void prepareTwoRestoredDatasets() throws Exception {

        Path dirOnFastStorage1 = getDirOnFastStorage(datasetIds.get(0));
        Path dirOnFastStorage2 = getDirOnFastStorage(datasetIds.get(1));

        testingClient.restore(sessionId, new DataSelection().addDataset(datasetIds.get(0))
                .addDataset(datasetIds.get(1)), 204);

        waitForIds();

        checkPresent(dirOnFastStorage1);
        checkPresent(dirOnFastStorage2);

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDataset(datasetIds.get(0))
                .addDataset(datasetIds.get(1)), Flag.NONE, 200);

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        Path preparedFile = setup.getPreparedCacheDir().resolve(preparedId);
        checkPresent(preparedFile);
    }

    @Test
    public void prepareRestoredDatafile() throws Exception {

        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        testingClient.restore(sessionId, new DataSelection().addDatafile(datafileIds.get(0)), 204);

        waitForIds();

        checkPresent(dirOnFastStorage);

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDatafile(datafileIds.get(0)),
                Flag.NONE, 200);

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        Path preparedFile = setup.getPreparedCacheDir().resolve(preparedId);
        checkPresent(preparedFile);
    }

    @Test
    public void prepareRestoredDatafileAndItsDataset() throws Exception {

        Path dirOnFastStorage = getDirOnFastStorage(datasetIds.get(0));

        testingClient.restore(sessionId,
                new DataSelection().addDatafile(datafileIds.get(0)).addDataset(datasetIds.get(0)), 204);
        waitForIds();
        checkPresent(dirOnFastStorage);

        String preparedId = testingClient.prepareData(sessionId, new DataSelection().addDatafile(datafileIds.get(0))
                .addDataset(datasetIds.get(0)), Flag.NONE, 200);

        while (!testingClient.isPrepared(preparedId, 200)) {
            Thread.sleep(1000);
        }

        checkPresent(setup.getPreparedCacheDir().resolve(preparedId));

    }

}
