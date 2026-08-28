package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PhotoImageComplicationData;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import java.time.Instant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DiasyncComplicationDataSourceServiceTest {
    @Test
    public void previewProvidesAllWatchfaceRepresentations() {
        DiasyncComplicationDataSourceService service =
                Robolectric.buildService(DiasyncComplicationDataSourceService.class)
                        .create()
                        .get();

        ComplicationData photo = service.getPreviewData(ComplicationType.PHOTO_IMAGE);
        ComplicationData status = service.getPreviewData(ComplicationType.SHORT_TEXT);
        ComplicationData data = service.getPreviewData(ComplicationType.LONG_TEXT);

        assertTrue(photo instanceof PhotoImageComplicationData);
        assertTrue(status instanceof ShortTextComplicationData);
        assertTrue(data instanceof LongTextComplicationData);
        assertEquals(" ", ((ShortTextComplicationData) status).getText()
                .getTextAt(service.getResources(), Instant.EPOCH).toString());
        assertEquals("6.7 ↗", ((LongTextComplicationData) data).getText()
                .getTextAt(service.getResources(), Instant.EPOCH).toString());
        assertNotNull(((LongTextComplicationData) data).getSmallImage());
    }

    @Test
    public void previewRejectsUnsupportedType() {
        DiasyncComplicationDataSourceService service =
                Robolectric.buildService(DiasyncComplicationDataSourceService.class)
                        .create()
                        .get();

        assertNull(service.getPreviewData(ComplicationType.RANGED_VALUE));
    }

}
