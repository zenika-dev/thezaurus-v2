package com.zenika.thezaurus.model;

import java.util.List;

public record EventTypeSummary(String name, String visibility, int total, List<CityCount> cities) {}
