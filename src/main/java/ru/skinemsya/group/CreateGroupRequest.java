package ru.skinemsya.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank(message = "Укажите название группы")
        @Size(max = 100, message = "Название должно быть не длиннее 100 символов")
        String name
) {
}
