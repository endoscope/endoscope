package org.endoscope.storage;

import org.endoscope.core.Stats;
import org.endoscope.util.DateUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.endoscope.core.PropertyTestUtil.withProperty;
import static org.endoscope.properties.Properties.SAVE_FREQ_MINUTES;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StatsCyclicWriterTest {
    @Mock
    StatsStorage statsStorage;

    @Mock
    DateUtil dateUtil;

    @Test
    public void should_save() throws Exception {
        withProperty(SAVE_FREQ_MINUTES, "10", () -> {
            given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000));

            StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(statsStorage, dateUtil);

            assertTrue(statsCyclicWriter.shouldSave());
            verify(dateUtil, times(2)).now();
            verifyNoMoreInteractions(dateUtil);
            verifyNoMoreInteractions(statsStorage);
        });
    }

    @Test
    public void should_not_save() throws Exception {
        withProperty(SAVE_FREQ_MINUTES, "10", () -> {
            given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000 -1));

            StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(statsStorage, dateUtil);

            assertFalse(statsCyclicWriter.shouldSave());
            verify(dateUtil, times(2)).now();
            verifyNoMoreInteractions(dateUtil);
            verifyNoMoreInteractions(statsStorage);
        });
    }

    @Test
    public void should_save_file_and_update_save_date() throws Exception {
        Date saveTime = new Date(13 * 60 * 1000);
        given(dateUtil.now()).willReturn(new Date(0), saveTime);

        StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(statsStorage, dateUtil);

        assertEquals(0, statsCyclicWriter.getLastSaveTime().getTime());

        Stats stats = new Stats();
        statsCyclicWriter.safeSave(stats);

        verify(statsStorage).save(same(stats));
        verifyNoMoreInteractions(statsStorage);
        verify(dateUtil, times(2)).now();
        verifyNoMoreInteractions(dateUtil);
        assertEquals(saveTime, statsCyclicWriter.getLastSaveTime());
    }
}