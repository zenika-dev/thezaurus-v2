package com.zenika.thezaurus.model;

import java.util.List;

public record TemplateContextPage(List<TemplateContextOption> options, String nextCursor) {}
