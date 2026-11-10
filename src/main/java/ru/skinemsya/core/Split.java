package ru.skinemsya.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Способ разделить трату между участниками.
 *
 * @param <P> идентификатор участника
 */
public sealed interface Split<P> permits Split.Equal, Split.Exact, Split.Shares {

    /** Возвращает долю каждого участника. Сумма долей всегда равна {@code total}. */
    Map<P, Money> apply(Money total);

    /** Поровну между участниками. */
    record Equal<P>(List<P> participants) implements Split<P> {

        public Equal {
            Objects.requireNonNull(participants, "participants");
            if (participants.isEmpty()) {
                throw new IllegalArgumentException("Нужен хотя бы один участник");
            }
            if (new HashSet<>(participants).size() != participants.size()) {
                throw new IllegalArgumentException("Участники не должны повторяться");
            }
            participants = List.copyOf(participants);
        }

        @Override
        public Map<P, Money> apply(Money total) {
            List<Money> parts = total.splitEvenly(participants.size());
            Map<P, Money> result = new LinkedHashMap<>();
            for (int i = 0; i < participants.size(); i++) {
                result.put(participants.get(i), parts.get(i));
            }
            return Collections.unmodifiableMap(result);
        }
    }

    /** Точные суммы для каждого участника. */
    record Exact<P>(Map<P, Money> amounts) implements Split<P> {

        public Exact {
            Objects.requireNonNull(amounts, "amounts");
            if (amounts.isEmpty()) {
                throw new IllegalArgumentException("Нужен хотя бы один участник");
            }
            if (amounts.values().stream().anyMatch(Money::isNegative)) {
                throw new IllegalArgumentException("Доля участника не может быть отрицательной");
            }
            amounts = Collections.unmodifiableMap(new LinkedHashMap<>(amounts));
        }

        @Override
        public Map<P, Money> apply(Money total) {
            Money sum = Money.sum(amounts.values());
            if (!sum.equals(total)) {
                throw new IllegalArgumentException(
                        "Сумма долей (%s) не совпадает с суммой траты (%s)".formatted(sum, total));
            }
            return amounts;
        }
    }

    /** Пропорционально долям, например 2:1:1. */
    record Shares<P>(Map<P, Integer> shares) implements Split<P> {

        public Shares {
            Objects.requireNonNull(shares, "shares");
            if (shares.isEmpty()) {
                throw new IllegalArgumentException("Нужен хотя бы один участник");
            }
            if (shares.values().stream().anyMatch(s -> s == null || s <= 0)) {
                throw new IllegalArgumentException("Доли должны быть положительными");
            }
            shares = Collections.unmodifiableMap(new LinkedHashMap<>(shares));
        }

        @Override
        public Map<P, Money> apply(Money total) {
            List<P> participants = List.copyOf(shares.keySet());
            List<Money> parts = total.allocate(List.copyOf(shares.values()));
            Map<P, Money> result = new LinkedHashMap<>();
            for (int i = 0; i < participants.size(); i++) {
                result.put(participants.get(i), parts.get(i));
            }
            return Collections.unmodifiableMap(result);
        }
    }
}
