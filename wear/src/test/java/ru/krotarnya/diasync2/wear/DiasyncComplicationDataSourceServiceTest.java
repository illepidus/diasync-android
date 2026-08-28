package ru.krotarnya.diasync2.wear;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
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
    public void previewProvidesRepresentativeShortText() {
        DiasyncComplicationDataSourceService service =
                Robolectric.buildService(DiasyncComplicationDataSourceService.class)
                        .create()
                        .get();

        ComplicationData data = service.getPreviewData(ComplicationType.SHORT_TEXT);

        assertTrue(data instanceof ShortTextComplicationData);
        ShortTextComplicationData shortText = (ShortTextComplicationData) data;
        assertEquals("6.7 →", shortText.getText()
                .getTextAt(service.getResources(), Instant.EPOCH).toString());
    }

    @Test
    public void previewRejectsUnsupportedType() {
        DiasyncComplicationDataSourceService service =
                Robolectric.buildService(DiasyncComplicationDataSourceService.class)
                        .create()
                        .get();

        assertNull(service.getPreviewData(ComplicationType.LONG_TEXT));
    }
}
