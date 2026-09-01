package com.zenika.thezaurus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zenika.thezaurus.model.Event;
import com.zenika.thezaurus.model.EventsDashboard;
import com.zenika.thezaurus.model.Location;
import com.zenika.thezaurus.model.Visibility;
import com.zenika.thezaurus.repository.EventRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class EventServiceTest {

    private EventRepository repository;
    private EventService service;

    @BeforeEach
    public void setUp() {
        repository = Mockito.mock(EventRepository.class);
        service = new EventService();
        service.repository = repository;
    }

    private static Event event(String type, Visibility visibility, String date, String city) {
        return new Event(null, type + " " + city, type, date, visibility, new Location(city, "France", null, null));
    }

    @Test
    public void testGetDashboardAggregatesByYearMonthAndType() throws Exception {
        Mockito.when(repository.findAllByYear(2026))
                .thenReturn(List.of(
                        event("NightClazz", Visibility.PRIVATE, "2026-01-15", "Nantes"),
                        event("NightClazz", Visibility.PRIVATE, "2026-01-20", "Nantes"),
                        event("Conférence", Visibility.PUBLIC, "2026-02-10", "Paris")));

        EventsDashboard dashboard = service.getDashboard(2026);

        assertEquals(2026, dashboard.year());
        assertEquals(2, dashboard.totals().internal());
        assertEquals(1, dashboard.totals().external());

        assertEquals(2, dashboard.monthly().get(0).internal());
        assertEquals(0, dashboard.monthly().get(0).external());
        assertEquals(0, dashboard.monthly().get(1).internal());
        assertEquals(1, dashboard.monthly().get(1).external());

        assertEquals(2, dashboard.eventTypes().size());
        var nightClazz = dashboard.eventTypes().stream()
                .filter(t -> t.name().equals("NightClazz"))
                .findFirst()
                .orElseThrow();
        assertEquals("internal", nightClazz.visibility());
        assertEquals(2, nightClazz.total());
        assertEquals(1, nightClazz.cities().size());
        assertEquals("Nantes", nightClazz.cities().get(0).city());
        assertEquals(2, nightClazz.cities().get(0).count());
    }

    @Test
    public void testGetDashboardIgnoresInvalidOrMissingDates() throws Exception {
        Event noDate = new Event(null, "Sans date", "Matinale", null, Visibility.PRIVATE, null);

        Event badDate = event("Matinale", Visibility.PRIVATE, "not-a-date", "Bordeaux");

        Mockito.when(repository.findAllByYear(2026)).thenReturn(List.of(noDate, badDate));

        EventsDashboard dashboard = service.getDashboard(2026);

        assertEquals(0, dashboard.totals().internal());
        assertEquals(0, dashboard.totals().external());
        assertEquals(0, dashboard.eventTypes().size());
    }
}
