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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class StatsPersistenceTest {
    private static final String APP_TYPE = "app-type";
    private static final String APP_INSTANCE = "app-instance";
    private static final int SAVE_FEQ = 10;
    private static final int DAYS_TO_KEEP = 0;
    private static final int ONE_MINUTE = 1;
    private static final long ONE_MINUTE_MS = 60*1000;
    @Mock
    Storage storage;

    @Mock
    DateUtil dateUtil;

    @Test
    public void should_save() throws Exception {
        given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000));

        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ, DAYS_TO_KEEP);

        assertTrue(statsPersistence.shouldSave());
        verify(dateUtil, times(2)).now();
        verifyNoMoreInteractions(dateUtil);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_not_save() throws Exception {
        given(dateUtil.now()).willReturn(new Date(0), new Date(10 * 60 * 1000 -1));

        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ, DAYS_TO_KEEP);

        assertFalse(statsPersistence.shouldSave());
        verify(dateUtil, times(2)).now();
        verifyNoMoreInteractions(dateUtil);
        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_save_file_and_update_save_date() throws Exception {
        Date saveTime = new Date(13 * 60 * 1000);
        given(dateUtil.now()).willReturn(new Date(0), saveTime);
        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, SAVE_FEQ, DAYS_TO_KEEP);

        assertEquals(0, statsPersistence.getLastSaveTime().getTime());

        Stats stats = new Stats();
        statsPersistence.safeSave(stats);

        verify(storage).save(same(stats), eq(APP_INSTANCE), eq(APP_TYPE));
        verifyNoMoreInteractions(storage);
        verify(dateUtil, times(4)).now();
        verifyNoMoreInteractions(dateUtil);
        assertEquals(saveTime, statsPersistence.getLastSaveTime());
    }

    @Test
    public void should_not_save_for_5_minutes_since_error() throws Exception {
        given(dateUtil.now()).willReturn(new Date(0));

        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, ONE_MINUTE, DAYS_TO_KEEP);
        assertFalse(statsPersistence.shouldSave());

        //it's time for first save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertTrue(statsPersistence.shouldSave());

        //this will fail and set last error date
        given(dateUtil.now()).willReturn(new Date(0));
        statsPersistence.safeSave(null);

        //let's go back to the moment where we should save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertFalse(statsPersistence.shouldSave());

        given(dateUtil.now()).willReturn(new Date( 3 * ONE_MINUTE_MS + 500));
        assertFalse(statsPersistence.shouldSave());

        given(dateUtil.now()).willReturn(new Date( 5 * ONE_MINUTE_MS + 500)); //5,5minutes
        assertTrue(statsPersistence.shouldSave());
    }

    @Test
    public void should_skip_cleanup_when_disabled(){
        given(dateUtil.now()).willReturn(new Date(0));

        int daysToKeep = 0;//disabled
        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, ONE_MINUTE, daysToKeep);

        statsPersistence.safeCleanup();

        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_skip_cleanup_when_last_save_failed(){
        given(dateUtil.now()).willReturn(new Date(0));
        given(storage.save(any(), any(), any())).willThrow(RuntimeException.class);

        int daysToKeep = 1;//enabled
        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, ONE_MINUTE, daysToKeep);
        //it's time for first save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertTrue(statsPersistence.shouldSave());

        //this will fail and set last error date
        given(dateUtil.now()).willReturn(new Date(0));
        statsPersistence.safeSave(null);
        assertFalse(statsPersistence.shouldSave());//locked due to error

        statsPersistence.safeCleanup();

        verifyNoMoreInteractions(storage);
    }

    @Test
    public void should_run_cleanup(){
        given(dateUtil.now()).willReturn(new Date(0));
        given(storage.save(any(), any(), any())).willThrow(RuntimeException.class);

        int daysToKeep = 1;//enabled
        StatsPersistence statsPersistence = new StatsPersistence(storage, dateUtil, APP_INSTANCE, APP_TYPE, ONE_MINUTE, daysToKeep);
        //it's time for first save
        given(dateUtil.now()).willReturn(new Date( 1 * ONE_MINUTE_MS + 500));
        assertTrue(statsPersistence.shouldSave());

        statsPersistence.safeCleanup();

        verify(storage).cleanup(eq(daysToKeep), eq(APP_TYPE));
        verifyNoMoreInteractions(storage);
    }
}