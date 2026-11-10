'use strict';

/*
 * Веб-интерфейс «Скинемся» на чистом JavaScript.
 * Работает поверх того же REST API, что описан в Swagger.
 */

const TOKEN_KEY = 'skinemsya.token';
const GROUP_KEY = 'skinemsya.group';

// ---------- Хранилище ----------

const storage = {
    get(key) {
        try {
            return localStorage.getItem(key);
        } catch {
            return null;
        }
    },
    set(key, value) {
        try {
            if (value == null) {
                localStorage.removeItem(key);
            } else {
                localStorage.setItem(key, value);
            }
        } catch {
            // приватный режим браузера — просто не запоминаем
        }
    },
};

const state = {
    token: storage.get(TOKEN_KEY),
    me: null,
    groups: [],
    groupId: null,
    data: null,
};

// ---------- API ----------

class ApiError extends Error {
    constructor(status, problem) {
        super(problem?.detail || `Ошибка ${status}`);
        this.status = status;
        this.errors = problem?.errors || {};
    }
}

async function api(method, path, body) {
    const headers = {};
    if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
    }
    if (state.token) {
        headers.Authorization = `Bearer ${state.token}`;
    }

    let response;
    try {
        response = await fetch(path, {
            method,
            headers,
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch {
        throw new ApiError(0, {detail: 'Сервер недоступен. Проверьте, что приложение запущено.'});
    }

    if (response.status === 401 && state.token) {
        forgetToken();
        throw new ApiError(401, {detail: 'Сессия устарела, начинаем заново'});
    }
    if (response.status === 204) {
        return null;
    }
    const text = await response.text();
    const data = text ? JSON.parse(text) : null;
    if (!response.ok) {
        throw new ApiError(response.status, data);
    }
    return data;
}

function messageOf(error) {
    const fieldErrors = Object.values(error.errors || {});
    return fieldErrors.length ? fieldErrors.join('. ') : error.message;
}

// ---------- Утилиты ----------

/** Создаёт DOM-элемент. Текст всегда вставляется как текст, поэтому XSS через имена невозможен. */
function h(tag, props, ...children) {
    const el = document.createElement(tag);
    for (const [key, value] of Object.entries(props || {})) {
        if (value == null || value === false) {
            continue;
        }
        if (key === 'class') {
            el.className = value;
        } else if (key.startsWith('on')) {
            el.addEventListener(key.slice(2), value);
        } else if (key === 'value' || key === 'checked') {
            el[key] = value;
        } else {
            el.setAttribute(key, value === true ? '' : value);
        }
    }
    for (const child of children.flat()) {
        if (child != null && child !== false) {
            el.append(child instanceof Node ? child : String(child));
        }
    }
    return el;
}

const wholeRubles = new Intl.NumberFormat('ru-RU', {style: 'currency', currency: 'RUB', maximumFractionDigits: 0});
const exactRubles = new Intl.NumberFormat('ru-RU', {style: 'currency', currency: 'RUB', minimumFractionDigits: 2});

function money(value) {
    const number = Number(value);
    return Number.isInteger(number) ? wholeRubles.format(number) : exactRubles.format(number);
}

function signedMoney(value) {
    const number = Number(value);
    return number > 0 ? `+${money(number)}` : money(number);
}

const dateFormat = new Intl.DateTimeFormat('ru-RU', {day: 'numeric', month: 'long'});

function formatDate(value) {
    return dateFormat.format(new Date(value));
}

function today() {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 10);
}

/** Копейки из строки ввода, чтобы не считать деньги во float. */
function toKopecks(value) {
    const number = Number(String(value).replace(',', '.'));
    return Number.isFinite(number) ? Math.round(number * 100) : NaN;
}

function fromKopecks(kopecks) {
    return kopecks / 100;
}

let toastTimer;

function toast(message, kind = 'info') {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = `toast show ${kind === 'error' ? 'error' : ''}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        el.className = 'toast';
    }, kind === 'error' ? 5000 : 2500);
}

/** Выполняет действие с блокировкой кнопки и показом ошибки. */
async function run(button, action) {
    if (button) {
        button.disabled = true;
    }
    try {
        await action();
    } catch (error) {
        toast(messageOf(error), 'error');
        if (error.status === 401) {
            start();
        }
    } finally {
        if (button) {
            button.disabled = false;
        }
    }
}

// ---------- Данные ----------

/**
 * Регистрация не нужна: при первом заходе создаём гостя и запоминаем его токен в браузере.
 * Если токен устарел или база очищена — тихо начинаем с новым гостем.
 */
async function ensureSession() {
    if (state.token) {
        try {
            await loadSession();
            return;
        } catch {
            forgetToken();
        }
    }
    const token = await api('POST', '/api/auth/guest', {displayName: 'Я'});
    state.token = token.accessToken;
    storage.set(TOKEN_KEY, state.token);
    await loadSession();
}

function forgetToken() {
    state.token = null;
    storage.set(TOKEN_KEY, null);
}

async function loadSession() {
    state.me = await api('GET', '/api/users/me');
    await loadGroups();
}

async function loadGroups(preferredId) {
    state.groups = await api('GET', '/api/groups');
    const saved = Number(preferredId ?? storage.get(GROUP_KEY));
    const exists = state.groups.some(group => group.id === saved);
    state.groupId = exists ? saved : state.groups[0]?.id ?? null;
    await loadGroup();
}

async function loadGroup() {
    if (state.groupId == null) {
        state.data = null;
        return;
    }
    storage.set(GROUP_KEY, String(state.groupId));
    const base = `/api/groups/${state.groupId}`;
    const [group, expenses, balances, transfers, settlements] = await Promise.all([
        api('GET', base),
        api('GET', `${base}/expenses`),
        api('GET', `${base}/balances`),
        api('GET', `${base}/settle-up`),
        api('GET', `${base}/settlements`),
    ]);
    state.data = {group, expenses, balances, transfers, settlements};
}

async function refresh() {
    await loadGroup();
    render();
}

// ---------- Отрисовка ----------

function render() {
    const root = document.getElementById('app');
    root.replaceChildren(state.me ? renderApp() : h('div', {class: 'splash'}, 'Загрузка…'));
}

function renderApp() {
    return h('div', null,
        h('header', {class: 'topbar'},
            h('div', {class: 'logo'}, '💸 Скинемся'),
            h('div', {class: 'muted tagline'}, 'Делите общие расходы без споров и таблиц')),
        h('div', {class: 'layout'},
            renderSidebar(),
            h('main', null, state.data ? renderGroup(state.data) : renderNoGroups())));
}

function renderSidebar() {
    const input = h('input', {class: 'input', placeholder: 'Новая группа', maxlength: 100, required: true});
    const button = h('button', {class: 'btn btn-primary', type: 'submit', title: 'Создать группу'}, '+');

    return h('aside', null,
        h('div', {class: 'card'},
            h('h2', {class: 'card-title'}, 'Группы'),
            state.groups.length
                ? h('ul', {class: 'group-list'}, state.groups.map(group => h('li', null,
                    h('button', {
                        class: `group-item ${group.id === state.groupId ? 'active' : ''}`,
                        onclick: () => run(null, async () => {
                            state.groupId = group.id;
                            await refresh();
                        }),
                    }, group.name))))
                : h('p', {class: 'empty'}, 'Пока нет групп'),
            h('form', {
                class: 'inline-form',
                onsubmit: event => {
                    event.preventDefault();
                    run(button, async () => {
                        const group = await api('POST', '/api/groups', {name: input.value.trim()});
                        await loadGroups(group.id);
                        render();
                        toast('Группа создана');
                    });
                },
            }, input, button)));
}

function renderNoGroups() {
    return h('div', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Создайте первую группу'),
        h('p', {class: 'muted'},
            'Например, «Поездка в Питер» или «Квартира». Добавьте участников, записывайте траты — ',
            'а мы посчитаем, кто кому сколько должен.'));
}

function renderGroup(data) {
    const {group} = data;
    const isOwner = group.ownerId === state.me.id;

    return h('div', null,
        h('div', {class: 'group-header'},
            h('div', null,
                h('h1', null, group.name),
                h('div', {class: 'muted'}, `${group.members.length} участн. · создана ${formatDate(group.createdAt)}`)),
            isOwner && h('button', {
                class: 'btn btn-small btn-danger',
                onclick: event => {
                    if (!confirm(`Удалить группу «${group.name}» со всеми тратами?`)) {
                        return;
                    }
                    run(event.currentTarget, async () => {
                        await api('DELETE', `/api/groups/${group.id}`);
                        await loadGroups();
                        render();
                        toast('Группа удалена');
                    });
                },
            }, 'Удалить группу')),
        h('div', {class: 'group-grid'},
            h('div', null, renderExpenseForm(data), renderExpenses(data)),
            h('div', null, renderSettleUp(data), renderBalances(data), renderMembers(data), renderSettlements(data))));
}

// ---------- Новая трата ----------

function renderExpenseForm({group}) {
    const members = group.members;
    const myMember = members.find(member => member.userId === state.me.id);
    let splitType = 'EQUAL';

    const description = h('input', {class: 'input', placeholder: 'Жильё, бензин, продукты…', maxlength: 200, required: true});
    const amount = h('input', {class: 'input', type: 'number', min: '0.01', step: '0.01', placeholder: '0', required: true});
    const payer = h('select', {class: 'input'},
        members.map(member => h('option', {value: member.id, selected: member.id === myMember?.id}, member.name)));
    const date = h('input', {class: 'input', type: 'date', value: today()});
    const splitBox = h('div', {class: 'form'});
    const hint = h('div', {class: 'split-hint'});
    const submit = h('button', {class: 'btn btn-primary', type: 'submit'}, 'Добавить трату');

    const rows = new Map();

    function drawSplit() {
        rows.clear();
        splitBox.replaceChildren(...members.map(member => {
            let control;
            if (splitType === 'EQUAL') {
                control = h('input', {type: 'checkbox', checked: true, oninput: updateHint});
                rows.set(member.id, control);
                return h('div', {class: 'split-row'}, h('label', null, control, member.name));
            }
            control = splitType === 'EXACT'
                ? h('input', {class: 'input input-small', type: 'number', min: '0', step: '0.01', placeholder: '0', oninput: updateHint})
                : h('input', {class: 'input input-small', type: 'number', min: '0', step: '1', value: '1', oninput: updateHint});
            rows.set(member.id, control);
            return h('div', {class: 'split-row'}, h('span', null, member.name),
                h('span', null, control, splitType === 'EXACT' ? ' ₽' : ' дол.'));
        }));
        updateHint();
    }

    function updateHint() {
        const total = toKopecks(amount.value || 0);
        hint.classList.remove('warn');
        if (!(total > 0) && splitType !== 'EXACT') {
            hint.textContent = 'Введите сумму, и мы покажем, сколько с каждого';
        } else if (splitType === 'EQUAL') {
            const count = [...rows.values()].filter(box => box.checked).length;
            hint.textContent = count
                ? `По ${money(fromKopecks(Math.floor(total / count)))} на каждого из ${count}`
                : 'Отметьте, на кого делить';
        } else if (splitType === 'EXACT') {
            const assigned = [...rows.values()].reduce((sum, input) => sum + (toKopecks(input.value || 0) || 0), 0);
            const left = total - assigned;
            hint.textContent = left === 0
                ? 'Сумма распределена полностью ✓'
                : `${left > 0 ? 'Осталось распределить' : 'Лишние'}: ${money(fromKopecks(Math.abs(left)))}`;
            hint.classList.toggle('warn', left !== 0);
        } else {
            const shares = [...rows.values()].reduce((sum, input) => sum + (Number(input.value) || 0), 0);
            hint.textContent = shares > 0
                ? `Одна доля ≈ ${money(fromKopecks(Math.floor(total / shares)))}`
                : 'Укажите доли участников';
        }
    }

    function buildSplit() {
        const entries = [...rows.entries()];
        if (splitType === 'EQUAL') {
            return {type: 'EQUAL', memberIds: entries.filter(([, box]) => box.checked).map(([id]) => id)};
        }
        if (splitType === 'EXACT') {
            return {
                type: 'EXACT',
                parts: entries.filter(([, input]) => Number(input.value) > 0)
                    .map(([id, input]) => ({memberId: id, amount: Number(input.value)})),
            };
        }
        return {
            type: 'SHARES',
            parts: entries.filter(([, input]) => Number(input.value) > 0)
                .map(([id, input]) => ({memberId: id, share: Math.round(Number(input.value))})),
        };
    }

    const typeButton = (type, title) => h('button', {
        type: 'button',
        class: type === splitType ? 'active' : '',
        onclick: event => {
            splitType = type;
            event.currentTarget.parentElement.querySelectorAll('button')
                .forEach(button => button.classList.toggle('active', button === event.currentTarget));
            drawSplit();
        },
    }, title);

    amount.addEventListener('input', updateHint);
    drawSplit();

    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Новая трата'),
        h('form', {
            class: 'form',
            onsubmit: event => {
                event.preventDefault();
                run(submit, async () => {
                    await api('POST', `/api/groups/${group.id}/expenses`, {
                        description: description.value.trim(),
                        amount: Number(amount.value),
                        payerId: Number(payer.value),
                        spentOn: date.value || null,
                        split: buildSplit(),
                    });
                    await refresh();
                    toast('Трата добавлена');
                });
            },
        },
        h('label', {class: 'field'}, h('span', null, 'На что'), description),
        h('div', {class: 'form-row'},
            h('label', {class: 'field'}, h('span', null, 'Сколько, ₽'), amount),
            h('label', {class: 'field'}, h('span', null, 'Кто платил'), payer),
            h('label', {class: 'field'}, h('span', null, 'Когда'), date)),
        h('div', {class: 'field'},
            h('span', null, 'Как делим'),
            h('div', {class: 'segmented'},
                typeButton('EQUAL', 'Поровну'),
                typeButton('EXACT', 'Суммами'),
                typeButton('SHARES', 'Долями'))),
        splitBox,
        hint,
        submit));
}

// ---------- Списки ----------

function renderExpenses({group, expenses}) {
    const total = expenses.reduce((sum, expense) => sum + toKopecks(expense.amount), 0);

    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Траты', expenses.length > 0 && h('small', null, `всего ${money(fromKopecks(total))}`)),
        expenses.length
            ? h('ul', {class: 'list'}, expenses.map(expense => h('li', null,
                h('div', {class: 'grow'},
                    h('div', {class: 'item-title'}, expense.description),
                    h('div', {class: 'item-sub'},
                        `${expense.payer.name} заплатил(а) · ${formatDate(expense.spentOn)} · `,
                        expense.shares.map(share => `${share.member.name} ${money(share.amount)}`).join(', '))),
                h('div', {class: 'amount'}, money(expense.amount)),
                h('button', {
                    class: 'icon-btn',
                    title: 'Удалить трату',
                    'aria-label': 'Удалить трату',
                    onclick: event => {
                        if (!confirm(`Удалить трату «${expense.description}»?`)) {
                            return;
                        }
                        run(event.currentTarget, async () => {
                            await api('DELETE', `/api/groups/${group.id}/expenses/${expense.id}`);
                            await refresh();
                        });
                    },
                }, '✕'))))
            : h('p', {class: 'empty'}, 'Трат пока нет. Добавьте первую выше.'));
}

function renderSettleUp({group, transfers, expenses}) {
    let content;
    if (transfers.length) {
        content = transfers.map(transfer => h('div', {class: 'transfer'},
            h('div', {class: 'transfer-names'},
                h('div', {class: 'item-title'}, `${transfer.from.name} → ${transfer.to.name}`),
                h('div', {class: 'amount'}, money(transfer.amount))),
            h('button', {
                class: 'btn btn-small',
                title: 'Записать, что перевод сделан',
                onclick: event => run(event.currentTarget, async () => {
                    await api('POST', `/api/groups/${group.id}/settlements`, {
                        fromMemberId: transfer.from.id,
                        toMemberId: transfer.to.id,
                        amount: Number(transfer.amount),
                    });
                    await refresh();
                    toast('Перевод отмечен');
                }),
            }, 'Перевёл')));
    } else {
        content = h('div', {class: 'all-settled'}, expenses.length ? 'Все в расчёте 🎉' : 'Пока никто никому не должен');
    }

    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Как рассчитаться',
            transfers.length > 0 && h('small', null, `${transfers.length} перевод(а)`)),
        content);
}

function renderBalances({balances}) {
    const max = Math.max(1, ...balances.map(balance => Math.abs(Number(balance.balance))));

    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Балансы', h('small', null, '+ должны вам, − должны вы')),
        balances.map(balance => {
            const value = Number(balance.balance);
            const kind = value > 0 ? 'pos' : value < 0 ? 'neg' : '';
            const width = `${(Math.abs(value) / max) * 50}%`;
            return h('div', {class: 'balance-row'},
                h('div', {class: 'balance-head'},
                    h('span', null, balance.member.name),
                    h('span', {class: `amount ${kind}`}, signedMoney(value))),
                h('div', {class: 'bar'}, kind && h('i', {class: kind, style: `width: ${width}`})));
        }));
}

function renderMembers({group}) {
    const name = h('input', {class: 'input', placeholder: 'Имя друга', maxlength: 100, required: true});
    const submit = h('button', {class: 'btn btn-primary', type: 'submit', title: 'Добавить участника'}, '+');

    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'Участники', h('small', null, String(group.members.length))),
        h('ul', {class: 'list'}, group.members.map(member => h('li', null,
            h('span', {class: 'grow'},
                member.name,
                member.userId === state.me.id && h('span', {class: 'badge badge-accent'}, 'вы'))))),
        h('form', {
            class: 'inline-form',
            style: 'margin-top: 12px',
            onsubmit: event => {
                event.preventDefault();
                run(submit, async () => {
                    await api('POST', `/api/groups/${group.id}/members`, {name: name.value.trim()});
                    await refresh();
                    toast('Участник добавлен');
                });
            },
        }, name, submit));
}

function renderSettlements({group, settlements}) {
    if (!settlements.length) {
        return null;
    }
    return h('section', {class: 'card'},
        h('h2', {class: 'card-title'}, 'История переводов'),
        h('ul', {class: 'list'}, settlements.map(settlement => h('li', null,
            h('div', {class: 'grow'},
                h('div', null, `${settlement.from.name} → ${settlement.to.name}`),
                h('div', {class: 'item-sub'}, formatDate(settlement.createdAt))),
            h('div', {class: 'amount'}, money(settlement.amount)),
            h('button', {
                class: 'icon-btn',
                title: 'Удалить перевод',
                'aria-label': 'Удалить перевод',
                onclick: event => {
                    if (!confirm('Удалить запись о переводе?')) {
                        return;
                    }
                    run(event.currentTarget, async () => {
                        await api('DELETE', `/api/groups/${group.id}/settlements/${settlement.id}`);
                        await refresh();
                    });
                },
            }, '✕')))));
}

// ---------- Старт ----------

async function start() {
    state.me = null;
    render();
    try {
        await ensureSession();
    } catch (error) {
        document.getElementById('app').replaceChildren(h('div', {class: 'splash'}, messageOf(error)));
        return;
    }
    render();
}

start();
