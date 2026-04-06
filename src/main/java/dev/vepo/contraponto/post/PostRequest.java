package dev.vepo.contraponto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(@NotBlank(message = "Title is required") @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters") String title,
                          @NotBlank(message = "Slug is required") @Size(min = 3, max = 100, message = "Slug must be between 3 and 100 characters") String slug,
                          String description,
                          @NotBlank(message = "Content is required") String content,
                          String author,
                          Long coverId,
                          boolean published) {}