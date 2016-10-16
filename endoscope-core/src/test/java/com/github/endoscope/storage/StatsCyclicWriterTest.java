package com.github.endoscope.storage;

import com.github.endoscope.core.Stats;
import com.github.endoscope.util.DateUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class StatsCyclicWriterTest {
    private static final String APP_TYPE = "app-type";
    private static final String APP_INSTANCE = "app-instance";
    private static final int SAVE_FEQ = 10;
    @Mock
    Storage storage;

    @Mock
    DateUtil dateUtil;

    @Test
    public void should_save() throws Exception {
        given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000));

        StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ);

        assertTrue(statsCyclicWriter.shouldSave());
        verify(dateUtil, times(2)).now();
        verifyNoMoreInteractions(dateUtil);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_not_save() throws Exception {
        given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000 -1));

        StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ);

        assertFalse(statsCyclicWriter.shouldSave());
        verify(dateUtil, times(2)).now();
        verifyNoMoreInteractions(dateUtil);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_save_file_and_update_save_date() throws Exception {
        Date saveTime = new Date(13 * 60 * 1000);
        given(dateUtil.now()).willReturn(new Date(0), saveTime);
        StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ);

        assertEquals(0, statsCyclicWriter.getLastSaveTime().getTime());

        Stats stats = new Stats();
        statsCyclicWriter.safeSave(stats);

        verify(storage).save(same(stats), eq(APP_INSTANCE), eq(APP_TYPE));
        verifyNoMoreInteractions(storage);
        verify(dateUtil, times(3)).now();
        verifyNoMoreInteractions(dateUtil);
        assertEquals(saveTime, statsCyclicWriter.getLastSaveTime());
    }

    @Test
    public void should_not_save_for_5_minutes_since_error() throws Exception {
        int ONE_MINUTE = 1;
        long ONE_MINUTE_MS = 60*1000;
        given(dateUtil.now()).willReturn(new Date(0));

        StatsCyclicWriter statsCyclicWriter = new StatsCyclicWriter(storage, dateUtil, APP_INSTANCE, APP_TYPE, ONE_MINUTE);
        assertFalse(statsCyclicWriter.shouldSave());

        //it's time for first save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertTrue(statsCyclicWriter.shouldSave());

        //this will fail and set last error date
        given(dateUtil.now()).willReturn(new Date(0));
        statsCyclicWriter.safeSave(null);

        //let's go back to the moment where we should save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertFalse(statsCyclicWriter.shouldSave());

        given(dateUtil.now()).willReturn(new Date( 3 * ONE_MINUTE_MS + 500));
        assertFalse(statsCyclicWriter.shouldSave());

        given(dateUtil.now()).willReturn(new Date( 5 * ONE_MINUTE_MS + 500)); //5,5minutes
        assertTrue(statsCyclicWriter.shouldSave());
    }
}