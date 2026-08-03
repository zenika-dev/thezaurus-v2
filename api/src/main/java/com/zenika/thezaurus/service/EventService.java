package com.zenika.thezaurus.service;

import com.zenika.thezaurus.model.CityCount;
import com.zenika.thezaurus.model.Event;
import com.zenika.thezaurus.model.EventTypeSummary;
import com.zenika.thezaurus.model.EventsDashboard;
import com.zenika.thezaurus.model.EventsTotals;
import com.zenika.thezaurus.model.Location;
import com.zenika.thezaurus.model.MonthLabel;
import com.zenika.thezaurus.model.MonthlyActivity;
import com.zenika.thezaurus.model.Visibility;
import com.zenika.thezaurus.repository.EventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class EventService {

    @Inject
    EventRepository repository;

    public List<Event> findAll() throws ExecutionException, InterruptedException {
        return repository.findAll();
    }

    public List<Event> findAll(Integer page, Integer size) throws ExecutionException, InterruptedException {
        return repository.findAll(page, size);
    }

    public Event findById(String id) throws ExecutionException, InterruptedException {
        return repository.findById(id);
    }

    public Event create(Event event) throws ExecutionException, InterruptedException {
        return repository.create(event);
    }

    public Event update(String id, Event event) throws ExecutionException, InterruptedException {
        Event existing = repository.findById(id);
        if (existing == null) {
            return null;
        }
        return repository.update(id, event);
    }

    public boolean delete(String id) throws ExecutionException, InterruptedException {
        Event existing = repository.findById(id);
        if (existing == null) {
            return false;
        }
        repository.delete(id);
        return true;
    }

    public EventsDashboard getDashboard(int year) throws ExecutionException, InterruptedException {
        List<EventWithMonth> eventsWithMonth = withParsedMonth(repository.findAllByYear(year));

        Map<MonthLabel, Integer> internalByMonth = countByMonth(eventsWithMonth, false);
        Map<MonthLabel, Integer> externalByMonth = countByMonth(eventsWithMonth, true);

        return new EventsDashboard(
                year,
                buildTotals(internalByMonth, externalByMonth),
                buildMonthlyActivity(internalByMonth, externalByMonth),
                buildEventTypeSummaries(eventsWithMonth));
    }

    private List<EventWithMonth> withParsedMonth(List<Event> events) {
        return events.stream()
                .flatMap(event -> {
                    LocalDate date = parseDate(event.date());
                    return date == null
                            ? Stream.empty()
                            : Stream.of(new EventWithMonth(event, MonthLabel.of(date.getMonthValue())));
                })
                .toList();
    }

    private Map<MonthLabel, Integer> countByMonth(List<EventWithMonth> eventsWithMonth, boolean external) {
        return eventsWithMonth.stream()
                .filter(eventWithMonth -> isExternal(eventWithMonth.event()) == external)
                .collect(Collectors.groupingBy(
                        EventWithMonth::month,
                        () -> new EnumMap<>(MonthLabel.class),
                        Collectors.summingInt(eventWithMonth -> 1)));
    }

    private EventsTotals buildTotals(Map<MonthLabel, Integer> internalByMonth, Map<MonthLabel, Integer> externalByMonth) {
        int totalInternal = internalByMonth.values().stream().mapToInt(Integer::intValue).sum();
        int totalExternal = externalByMonth.values().stream().mapToInt(Integer::intValue).sum();
        return new EventsTotals(totalInternal, totalExternal);
    }

    private List<MonthlyActivity> buildMonthlyActivity(Map<MonthLabel, Integer> internalByMonth, Map<MonthLabel, Integer> externalByMonth) {
        return Arrays.stream(MonthLabel.values())
                .map(month -> new MonthlyActivity(
                        month,
                        internalByMonth.getOrDefault(month, 0),
                        externalByMonth.getOrDefault(month, 0)))
                .collect(Collectors.toList());
    }

    private List<EventTypeSummary> buildEventTypeSummaries(List<EventWithMonth> eventsWithMonth) {
        return eventsWithMonth.stream()
                .map(EventWithMonth::event)
                .collect(Collectors.groupingBy(Event::type, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toEventTypeSummary(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(EventTypeSummary::total).reversed())
                .collect(Collectors.toList());
    }

    private boolean isExternal(Event event) {
        return event.visibility() == Visibility.PUBLIC;
    }

    private EventTypeSummary toEventTypeSummary(String type, List<Event> typeEvents) {
        String visibility = isExternal(typeEvents.get(0)) ? "external" : "internal";

        List<CityCount> cities = typeEvents.stream()
                .map(Event::location)
                .filter(location -> location != null && location.city() != null && !location.city().isBlank())
                .collect(Collectors.groupingBy(Location::city, LinkedHashMap::new, Collectors.summingInt(location -> 1)))
                .entrySet().stream()
                .map(entry -> new CityCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(CityCount::count).reversed())
                .collect(Collectors.toList());

        return new EventTypeSummary(type, visibility, typeEvents.size(), cities);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private record EventWithMonth(Event event, MonthLabel month) {
    }
}
