-- Гостевые пользователи: веб-интерфейс работает без регистрации.
-- У гостя нет email и пароля, он входит по долгоживущему токену из браузера.
alter table app_user alter column email drop not null;
alter table app_user alter column password_hash drop not null;
