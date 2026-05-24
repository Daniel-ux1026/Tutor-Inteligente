const TOPICS = [
  "Números y operaciones",
  "Álgebra",
  "Geometría",
  "Funciones",
  "Estadística"
];

const DIFFICULTIES = ["basico", "intermedio", "avanzado"];
const DIFFICULTY_LABELS = {
  basico: "Básico",
  intermedio: "Intermedio",
  avanzado: "Avanzado"
};

const STUDENT_POOL = [
  "Ana Torres",
  "Luis Quispe",
  "María Huamán",
  "Diego Ramos",
  "Valeria Ccopa",
  "Kevin Flores"
];

const DB_NAME = "tutor-inteligente-pwa";
const DB_VERSION = 2;
const AUTH_TIMEOUT_SECONDS = 5 * 60;
const LOCKOUT_MS = 5 * 60 * 1000;
const MAX_LOGIN_ATTEMPTS = 5;
const DEMO_ACCOUNTS = [
  {
    id: "student:alumno@demo.pe",
    role: "student",
    name: "Ana Torres",
    email: "alumno@demo.pe",
    grade: 1,
    passwordHash: passwordHash("1234")
  },
  {
    id: "teacher:docente@demo.pe",
    role: "teacher",
    name: "Prof. Carlos Medina",
    email: "docente@demo.pe",
    grade: null,
    passwordHash: passwordHash("1234")
  }
];

let db;
let deferredInstallPrompt;
let authTimerId;
let authSecondsLeft = AUTH_TIMEOUT_SECONDS;

const state = {
  role: "student",
  studentName: "Ana Torres",
  grade: 1,
  topic: TOPICS[0],
  selectedOption: null,
  readyForNext: false,
  currentQuestionId: null,
  attempts: [],
  customQuestions: [],
  accounts: [],
  levels: {},
  streaks: {},
  forceOffline: false,
  authMode: "login",
  session: null,
  persistSession: false,
  rememberedLogin: null,
  loginFailures: {},
  lockouts: {},
  recovery: null
};

const el = {};

document.addEventListener("DOMContentLoaded", init);

async function init() {
  bindElements();
  fillTopicSelectors();
  bindEvents();

  db = await openDatabase();
  await loadStoredState();
  await ensureDemoAccounts();

  renderAll();
  pickQuestion();
  startAuthTimer();
  registerServiceWorker();
}

function bindElements() {
  [
    "connectionStatus",
    "installButton",
    "teacherProfileMenu",
    "teacherProfileButton",
    "teacherProfileName",
    "teacherProfileDropdown",
    "teacherDropdownLogout",
    "authStage",
    "appWorkspace",
    "loginModeButton",
    "registerModeButton",
    "authForm",
    "authNameField",
    "authName",
    "authEmail",
    "authPassword",
    "authGradeField",
    "authGrade",
    "rememberPassword",
    "forgotPasswordButton",
    "authTimer",
    "attemptStatus",
    "authMessage",
    "authSubmitButton",
    "recoveryForm",
    "recoveryEmail",
    "sendRecoveryButton",
    "recoveryStep",
    "recoveryCode",
    "recoveryNewPassword",
    "resetPasswordButton",
    "recoveryMessage",
    "backToLoginButton",
    "studentTab",
    "teacherTab",
    "studentSettings",
    "studentName",
    "gradeSelect",
    "topicSelect",
    "localAnswerCount",
    "syncedAnswerCount",
    "studentView",
    "teacherView",
    "gradeBadge",
    "difficultyBadge",
    "topicBadge",
    "questionText",
    "optionsList",
    "feedbackBox",
    "skipButton",
    "submitButton",
    "scoreRing",
    "accuracyValue",
    "correctStreak",
    "wrongStreak",
    "currentLevel",
    "recommendation",
    "offlineToggle",
    "pendingCount",
    "syncButton",
    "studentLogoutButton",
    "teacherLogoutButton",
    "bankCount",
    "riskCount",
    "questionForm",
    "newQuestionText",
    "newQuestionGrade",
    "newQuestionDifficulty",
    "newQuestionTopic",
    "newQuestionAnswer",
    "newQuestionHint",
    "formMessage",
    "topicBars",
    "studentReportBody",
    "customCount",
    "recentQuestions"
  ].forEach((id) => {
    el[id] = document.getElementById(id);
  });

  el.newOptionInputs = Array.from(document.querySelectorAll(".new-option"));
  el.authRoleInputs = Array.from(document.querySelectorAll('input[name="authRole"]'));
  el.roleTabs = document.querySelector(".role-tabs");
  el.profileMenuButtons = Array.from(document.querySelectorAll(".profile-dropdown button:not(.dropdown-logout)"));
}

function fillTopicSelectors() {
  TOPICS.forEach((topic) => {
    const optionA = new Option(topic, topic);
    const optionB = new Option(topic, topic);
    el.topicSelect.append(optionA);
    el.newQuestionTopic.append(optionB);
  });
}

function bindEvents() {
  el.loginModeButton.addEventListener("click", () => setAuthMode("login"));
  el.registerModeButton.addEventListener("click", () => setAuthMode("register"));
  el.authRoleInputs.forEach((input) => input.addEventListener("change", () => {
    updateAuthRoleFields();
    renderAttemptStatus();
  }));
  el.authForm.addEventListener("submit", handleAuthSubmit);
  el.authForm.addEventListener("input", renderAttemptStatus);
  el.forgotPasswordButton.addEventListener("click", showRecoveryForm);
  el.backToLoginButton.addEventListener("click", hideRecoveryForm);
  el.sendRecoveryButton.addEventListener("click", sendRecoveryLink);
  el.resetPasswordButton.addEventListener("click", resetPasswordWithCode);
  el.studentLogoutButton.addEventListener("click", logout);
  el.teacherLogoutButton.addEventListener("click", logout);
  el.teacherDropdownLogout.addEventListener("click", logout);
  el.teacherProfileButton.addEventListener("click", toggleTeacherProfile);
  el.profileMenuButtons.forEach((button) => {
    button.addEventListener("click", () => {
      el.teacherProfileDropdown.hidden = true;
      el.teacherProfileButton.setAttribute("aria-expanded", "false");
    });
  });
  document.addEventListener("click", (event) => {
    if (!el.teacherProfileMenu.contains(event.target)) {
      el.teacherProfileDropdown.hidden = true;
      el.teacherProfileButton.setAttribute("aria-expanded", "false");
    }
  });

  el.studentTab.addEventListener("click", () => setRole("student"));
  el.teacherTab.addEventListener("click", () => setRole("teacher"));

  el.studentName.addEventListener("input", async () => {
    state.studentName = el.studentName.value.trim() || "Estudiante";
    await saveMeta();
    renderReports();
  });

  el.gradeSelect.addEventListener("change", async () => {
    state.grade = Number(el.gradeSelect.value);
    await saveMeta();
    pickQuestion();
  });

  el.topicSelect.addEventListener("change", async () => {
    state.topic = el.topicSelect.value;
    await saveMeta();
    pickQuestion();
  });

  el.skipButton.addEventListener("click", () => pickQuestion(true));
  el.submitButton.addEventListener("click", handleSubmitAnswer);

  el.offlineToggle.addEventListener("change", async () => {
    state.forceOffline = el.offlineToggle.checked;
    await saveMeta();
    updateConnectionStatus();
  });

  el.syncButton.addEventListener("click", syncPendingAttempts);
  el.questionForm.addEventListener("submit", saveCustomQuestion);

  window.addEventListener("online", updateConnectionStatus);
  window.addEventListener("offline", updateConnectionStatus);

  window.addEventListener("beforeinstallprompt", (event) => {
    event.preventDefault();
    deferredInstallPrompt = event;
    el.installButton.hidden = false;
  });

  el.installButton.addEventListener("click", async () => {
    if (!deferredInstallPrompt) return;
    deferredInstallPrompt.prompt();
    await deferredInstallPrompt.userChoice;
    deferredInstallPrompt = null;
    el.installButton.hidden = true;
  });
}

function setRole(role) {
  if (state.session && role !== state.session.role) return;
  state.role = role;
  const isStudent = role === "student";
  el.studentView.hidden = !isStudent;
  el.teacherView.hidden = isStudent;
  el.appWorkspace.classList.toggle("teacher-mode", !isStudent);
  el.appWorkspace.classList.toggle("student-mode", isStudent);
  el.studentTab.classList.toggle("is-active", isStudent);
  el.teacherTab.classList.toggle("is-active", !isStudent);
  el.studentTab.setAttribute("aria-selected", String(isStudent));
  el.teacherTab.setAttribute("aria-selected", String(!isStudent));
}

function selectedAuthRole() {
  return el.authRoleInputs.find((input) => input.checked)?.value || "student";
}

function setAuthRole(role) {
  el.authRoleInputs.forEach((input) => {
    input.checked = input.value === role;
  });
  updateAuthRoleFields();
}

function setAuthMode(mode) {
  state.authMode = mode;
  const isLogin = mode === "login";
  el.loginModeButton.classList.toggle("is-active", isLogin);
  el.registerModeButton.classList.toggle("is-active", !isLogin);
  el.loginModeButton.setAttribute("aria-selected", String(isLogin));
  el.registerModeButton.setAttribute("aria-selected", String(!isLogin));
  el.authNameField.hidden = isLogin;
  el.authSubmitButton.textContent = isLogin ? "Ingresar" : "Registrarse";
  el.authPassword.autocomplete = isLogin ? "current-password" : "new-password";
  el.authMessage.textContent = "";
  hideRecoveryForm();
  resetAuthTimer();
  renderAttemptStatus();
}

function updateAuthRoleFields() {
  const role = selectedAuthRole();
  el.authGradeField.hidden = role !== "student";
}

async function handleAuthSubmit(event) {
  event.preventDefault();
  const role = selectedAuthRole();
  const email = normalizeEmail(el.authEmail.value);
  const password = el.authPassword.value;
  const key = loginKey(role, email);

  if (!email || !password) {
    showAuthMessage("Completa correo y contraseña para continuar.", "bad");
    return;
  }

  if (isLocked(key)) {
    showAuthMessage(`Cuenta bloqueada. Intenta nuevamente en ${formatSeconds(lockoutSeconds(key))}.`, "bad");
    renderAttemptStatus();
    return;
  }

  if (state.authMode === "register") {
    await registerAccount(role, email, password);
    return;
  }

  const account = findAccount(role, email);
  if (!account || account.passwordHash !== passwordHash(password)) {
    await registerFailedAttempt(key);
    return;
  }

  clearAuthFailure(key);
  await loginAccount(account, el.rememberPassword.checked);
}

async function registerAccount(role, email, password) {
  if (findAccount(role, email)) {
    showAuthMessage("Ese correo ya está registrado para este rol.", "bad");
    return;
  }

  const account = {
    id: loginKey(role, email),
    role,
    name: (el.authName.value.trim() || email.split("@")[0]).replace(/\s+/g, " "),
    email,
    grade: role === "student" ? Number(el.authGrade.value) : null,
    passwordHash: passwordHash(password)
  };

  state.accounts.push(account);
  await putRecord("accounts", account);
  showAuthMessage("Registro creado. Abriendo tu dashboard...", "good");
  await loginAccount(account, el.rememberPassword.checked);
}

async function loginAccount(account, remember) {
  state.session = {
    role: account.role,
    email: account.email,
    name: account.name,
    grade: account.grade,
    loggedAt: new Date().toISOString()
  };
  state.persistSession = remember;
  state.rememberedLogin = remember ? { role: account.role, email: account.email } : null;
  state.role = account.role;

  if (account.role === "student") {
    state.studentName = account.name;
    state.grade = account.grade || 1;
  }

  await saveMeta();
  renderAll();
  pickQuestion();
}

async function logout() {
  state.session = null;
  state.persistSession = false;
  el.teacherProfileDropdown.hidden = true;
  el.teacherProfileButton.setAttribute("aria-expanded", "false");
  await saveMeta();
  resetAuthTimer();
  renderAll();
}

async function registerFailedAttempt(key) {
  const failures = (state.loginFailures[key] || 0) + 1;
  state.loginFailures[key] = failures;

  if (failures >= MAX_LOGIN_ATTEMPTS) {
    state.lockouts[key] = Date.now() + LOCKOUT_MS;
    state.loginFailures[key] = 0;
    showAuthMessage("Llegaste a 5 intentos. La cuenta queda bloqueada por 5 minutos.", "bad");
  } else {
    showAuthMessage(`Contraseña incorrecta. Te quedan ${MAX_LOGIN_ATTEMPTS - failures} intentos.`, "bad");
  }

  await saveMeta();
  renderAttemptStatus();
}

function clearAuthFailure(key) {
  delete state.loginFailures[key];
  delete state.lockouts[key];
}

function loginKey(role, email) {
  return `${role}:${normalizeEmail(email)}`;
}

function normalizeEmail(email) {
  return String(email || "").trim().toLowerCase();
}

function findAccount(role, email) {
  const normalized = normalizeEmail(email);
  return state.accounts.find((account) => account.role === role && account.email === normalized);
}

function passwordHash(password) {
  let hash = 5381;
  String(password).split("").forEach((char) => {
    hash = ((hash << 5) + hash) + char.charCodeAt(0);
    hash &= 0xffffffff;
  });
  return `demo-${Math.abs(hash)}`;
}

function showAuthMessage(message, tone = "normal") {
  el.authMessage.textContent = message;
  el.authMessage.classList.toggle("is-good", tone === "good");
  el.authMessage.classList.toggle("is-bad", tone === "bad");
}

function startAuthTimer() {
  clearInterval(authTimerId);
  updateAuthTimerDisplay();
  authTimerId = setInterval(() => {
    renderAttemptStatus();
    if (state.session || !el.authStage || el.authStage.hidden) return;
    authSecondsLeft -= 1;
    if (authSecondsLeft <= 0) {
      el.authPassword.value = "";
      el.authName.value = "";
      showAuthMessage("Pasaron 5 minutos sin completar los datos. El formulario se actualizó.", "bad");
      resetAuthTimer();
      return;
    }
    updateAuthTimerDisplay();
  }, 1000);
}

function resetAuthTimer() {
  authSecondsLeft = AUTH_TIMEOUT_SECONDS;
  updateAuthTimerDisplay();
}

function updateAuthTimerDisplay() {
  el.authTimer.textContent = formatSeconds(authSecondsLeft);
}

function renderAttemptStatus() {
  const role = selectedAuthRole();
  const email = normalizeEmail(el.authEmail.value);
  const key = loginKey(role, email);

  if (!email) {
    el.attemptStatus.textContent = `${MAX_LOGIN_ATTEMPTS} intentos disponibles`;
    el.authSubmitButton.disabled = false;
    return;
  }

  if (isLocked(key)) {
    el.attemptStatus.textContent = `Bloqueado por ${formatSeconds(lockoutSeconds(key))}`;
    el.authSubmitButton.disabled = true;
    return;
  }

  const used = state.loginFailures[key] || 0;
  el.attemptStatus.textContent = `${MAX_LOGIN_ATTEMPTS - used} intentos disponibles`;
  el.authSubmitButton.disabled = false;
}

function isLocked(key) {
  const until = state.lockouts[key];
  if (!until) return false;
  if (Date.now() >= until) {
    delete state.lockouts[key];
    delete state.loginFailures[key];
    saveMeta();
    return false;
  }
  return true;
}

function lockoutSeconds(key) {
  return Math.max(0, Math.ceil(((state.lockouts[key] || 0) - Date.now()) / 1000));
}

function formatSeconds(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, "0");
  const seconds = Math.max(0, totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function showRecoveryForm() {
  el.authForm.hidden = true;
  el.recoveryForm.hidden = false;
  el.recoveryEmail.value = el.authEmail.value;
  el.recoveryMessage.textContent = "";
  el.recoveryStep.hidden = true;
  resetAuthTimer();
}

function hideRecoveryForm() {
  el.authForm.hidden = false;
  el.recoveryForm.hidden = true;
}

function sendRecoveryLink() {
  const email = normalizeEmail(el.recoveryEmail.value);
  const account = state.accounts.find((item) => item.email === email);

  if (!account) {
    el.recoveryMessage.textContent = "No encontramos una cuenta con ese correo.";
    el.recoveryMessage.className = "form-message is-bad";
    return;
  }

  const code = String(Math.floor(100000 + Math.random() * 900000));
  state.recovery = {
    email,
    accountId: account.id,
    code,
    expiresAt: Date.now() + LOCKOUT_MS,
    link: `https://tutor-demo.local/recuperar?token=${Date.now().toString(36)}`
  };
  el.recoveryStep.hidden = false;
  el.recoveryMessage.className = "form-message is-good";
  el.recoveryMessage.textContent = `Enlace de recuperación simulado para ${email}. Código 2FA: ${code}. Vence en 5 minutos.`;
}

async function resetPasswordWithCode() {
  if (!state.recovery || Date.now() > state.recovery.expiresAt) {
    el.recoveryMessage.className = "form-message is-bad";
    el.recoveryMessage.textContent = "El enlace de recuperación venció. Solicita uno nuevo.";
    return;
  }

  if (el.recoveryCode.value.trim() !== state.recovery.code) {
    el.recoveryMessage.className = "form-message is-bad";
    el.recoveryMessage.textContent = "El código de doble autenticación no coincide.";
    return;
  }

  const password = el.recoveryNewPassword.value;
  if (password.length < 4) {
    el.recoveryMessage.className = "form-message is-bad";
    el.recoveryMessage.textContent = "La nueva contraseña debe tener al menos 4 caracteres.";
    return;
  }

  const account = state.accounts.find((item) => item.id === state.recovery.accountId);
  if (!account) return;

  account.passwordHash = passwordHash(password);
  await putRecord("accounts", account);
  delete state.lockouts[account.id];
  delete state.loginFailures[account.id];
  state.recovery = null;
  await saveMeta();

  el.recoveryMessage.className = "form-message is-good";
  el.recoveryMessage.textContent = "Contraseña actualizada. Ya puedes ingresar.";
  el.authEmail.value = account.email;
  setAuthRole(account.role);
  setTimeout(() => {
    hideRecoveryForm();
    setAuthMode("login");
  }, 900);
}

function toggleTeacherProfile(event) {
  event.stopPropagation();
  const open = el.teacherProfileDropdown.hidden;
  el.teacherProfileDropdown.hidden = !open;
  el.teacherProfileButton.setAttribute("aria-expanded", String(open));
}

function buildQuestionBank() {
  const bank = [];

  for (let grade = 1; grade <= 5; grade += 1) {
    addNumberQuestions(bank, grade);
    addAlgebraQuestions(bank, grade);
    addGeometryQuestions(bank, grade);
    addFunctionQuestions(bank, grade);
    addStatisticsQuestions(bank, grade);
  }

  return bank;
}

function question(id, grade, topic, difficulty, text, answer, hint, salt = 0) {
  const { options, answerIndex } = optionSet(answer, grade * 13 + salt + DIFFICULTIES.indexOf(difficulty));
  return { id, grade, topic, difficulty, text, options, answerIndex, hint };
}

function optionSet(answer, salt) {
  const numericAnswer = Number(answer);
  const candidates = [
    numericAnswer,
    numericAnswer + 1 + (salt % 3),
    numericAnswer - 2 - (salt % 2),
    numericAnswer + 5 + (salt % 4),
    numericAnswer - 6
  ];
  const unique = [];

  candidates.forEach((value) => {
    if (!unique.includes(value)) unique.push(value);
  });

  while (unique.length < 4) {
    unique.push(numericAnswer + unique.length * 7);
  }

  const distractors = unique.filter((value) => value !== numericAnswer).slice(0, 3);
  const answerIndex = salt % 4;
  const options = [];

  for (let index = 0; index < 4; index += 1) {
    options[index] = index === answerIndex ? numericAnswer : distractors.shift();
  }

  return {
    options: options.map((value) => String(value)),
    answerIndex
  };
}

function addNumberQuestions(bank, grade) {
  const base = 12 + grade * 4;
  bank.push(question(
    `g${grade}-num-basico`,
    grade,
    TOPICS[0],
    "basico",
    `Calcula ${base} + ${8 + grade * 3}.`,
    base + 8 + grade * 3,
    "Suma primero decenas y luego unidades.",
    1
  ));
  bank.push(question(
    `g${grade}-num-intermedio`,
    grade,
    TOPICS[0],
    "intermedio",
    `Resuelve ${grade + 5} x ${grade + 4} - ${grade + 6}.`,
    (grade + 5) * (grade + 4) - (grade + 6),
    "Respeta el orden de operaciones: multiplicación antes de resta.",
    2
  ));
  bank.push(question(
    `g${grade}-num-avanzado`,
    grade,
    TOPICS[0],
    "avanzado",
    `Una familia reparte ${(grade + 3) * (grade + 8)} soles en partes iguales entre ${grade + 3} estudiantes. ¿Cuánto recibe cada uno?`,
    grade + 8,
    "Divide el total entre la cantidad de estudiantes.",
    3
  ));
}

function addAlgebraQuestions(bank, grade) {
  bank.push(question(
    `g${grade}-alg-basico`,
    grade,
    TOPICS[1],
    "basico",
    `Si x + ${grade + 4} = ${grade * 5 + 13}, ¿cuánto vale x?`,
    grade * 5 + 13 - (grade + 4),
    "Resta el mismo número a ambos lados.",
    5
  ));
  bank.push(question(
    `g${grade}-alg-intermedio`,
    grade,
    TOPICS[1],
    "intermedio",
    `Resuelve 2x + ${grade + 3} = ${2 * (grade + 6) + grade + 3}.`,
    grade + 6,
    "Primero resta el término independiente y luego divide entre 2.",
    6
  ));
  bank.push(question(
    `g${grade}-alg-avanzado`,
    grade,
    TOPICS[1],
    "avanzado",
    `Resuelve 3(x - ${grade}) = ${grade * 9 + 18}.`,
    (grade * 9 + 18) / 3 + grade,
    "Divide entre 3 y luego despeja x.",
    7
  ));
}

function addGeometryQuestions(bank, grade) {
  const width = grade + 5;
  const height = grade + 3;
  bank.push(question(
    `g${grade}-geo-basico`,
    grade,
    TOPICS[2],
    "basico",
    `Un rectángulo mide ${width} cm de largo y ${height} cm de ancho. ¿Cuál es su área?`,
    width * height,
    "El área de un rectángulo es largo por ancho.",
    9
  ));
  bank.push(question(
    `g${grade}-geo-intermedio`,
    grade,
    TOPICS[2],
    "intermedio",
    `Un triángulo tiene base ${2 * (grade + 4)} cm y altura ${grade + 3} cm. ¿Cuál es su área?`,
    (grade + 4) * (grade + 3),
    "Multiplica base por altura y divide entre 2.",
    10
  ));
  bank.push(question(
    `g${grade}-geo-avanzado`,
    grade,
    TOPICS[2],
    "avanzado",
    `Un prisma rectangular mide ${grade + 2} cm, ${grade + 3} cm y ${grade + 4} cm. ¿Cuál es su volumen?`,
    (grade + 2) * (grade + 3) * (grade + 4),
    "Multiplica las tres dimensiones.",
    11
  ));
}

function addFunctionQuestions(bank, grade) {
  bank.push(question(
    `g${grade}-fun-basico`,
    grade,
    TOPICS[3],
    "basico",
    `Si f(x) = x + ${grade + 2}, calcula f(${grade + 6}).`,
    grade + 6 + grade + 2,
    "Reemplaza x por el valor indicado.",
    13
  ));
  bank.push(question(
    `g${grade}-fun-intermedio`,
    grade,
    TOPICS[3],
    "intermedio",
    `Si f(x) = 2x - ${grade + 1}, calcula f(${grade + 7}).`,
    2 * (grade + 7) - (grade + 1),
    "Multiplica por 2 y luego resta.",
    14
  ));
  bank.push(question(
    `g${grade}-fun-avanzado`,
    grade,
    TOPICS[3],
    "avanzado",
    `La función lineal pasa por (0, ${grade + 2}) y (2, ${grade + 8}). ¿Cuál es su pendiente?`,
    ((grade + 8) - (grade + 2)) / 2,
    "Pendiente = cambio en y dividido entre cambio en x.",
    15
  ));
}

function addStatisticsQuestions(bank, grade) {
  const values = [grade + 8, grade + 12, grade + 16];
  bank.push(question(
    `g${grade}-est-basico`,
    grade,
    TOPICS[4],
    "basico",
    `Calcula el promedio de ${values.join(", ")}.`,
    values.reduce((sum, value) => sum + value, 0) / values.length,
    "Suma los datos y divide entre la cantidad de datos.",
    17
  ));
  bank.push(question(
    `g${grade}-est-intermedio`,
    grade,
    TOPICS[4],
    "intermedio",
    `Ordena mentalmente ${grade + 4}, ${grade + 12}, ${grade + 8}, ${grade + 16}, ${grade + 20}. ¿Cuál es la mediana?`,
    grade + 12,
    "La mediana es el dato del centro cuando están ordenados.",
    18
  ));
  bank.push(question(
    `g${grade}-est-avanzado`,
    grade,
    TOPICS[4],
    "avanzado",
    `En una bolsa hay ${grade + 3} fichas rojas y ${grade + 7} azules. Si se extrae una ficha, ¿cuántos resultados posibles hay?`,
    (grade + 3) + (grade + 7),
    "Cuenta todos los casos posibles.",
    19
  ));
}

const seedQuestions = buildQuestionBank();

function getAllQuestions() {
  return [...seedQuestions, ...state.customQuestions];
}

function levelKey() {
  return `${state.studentName}|${state.grade}|${state.topic}`;
}

function getDifficulty() {
  return state.levels[levelKey()] || "basico";
}

function setDifficulty(difficulty) {
  state.levels[levelKey()] = difficulty;
}

function getStreak() {
  const key = levelKey();
  if (!state.streaks[key]) {
    state.streaks[key] = { correct: 0, wrong: 0 };
  }
  return state.streaks[key];
}

function pickQuestion(forceDifferent = false) {
  const difficulty = getDifficulty();
  let pool = getAllQuestions().filter((item) => (
    item.grade === state.grade &&
    item.topic === state.topic &&
    item.difficulty === difficulty
  ));

  if (!pool.length) {
    pool = getAllQuestions().filter((item) => item.grade === state.grade && item.topic === state.topic);
  }

  if (!pool.length) {
    pool = getAllQuestions().filter((item) => item.grade === state.grade);
  }

  const attemptsInScope = state.attempts.filter((item) => (
    item.studentName === state.studentName &&
    item.grade === state.grade &&
    item.topic === state.topic
  )).length;
  let index = attemptsInScope % pool.length;

  if (forceDifferent && pool.length > 1) {
    const currentIndex = pool.findIndex((item) => item.id === state.currentQuestionId);
    index = (currentIndex + 1) % pool.length;
  }

  state.currentQuestionId = pool[index]?.id || seedQuestions[0].id;
  state.selectedOption = null;
  state.readyForNext = false;
  el.feedbackBox.textContent = "Selecciona una alternativa y comprueba tu respuesta.";
  el.feedbackBox.className = "feedback";
  renderStudent();
}

async function handleSubmitAnswer() {
  if (state.readyForNext) {
    pickQuestion();
    return;
  }

  if (state.selectedOption === null) {
    el.feedbackBox.textContent = "Marca una alternativa para continuar.";
    el.feedbackBox.className = "feedback is-bad";
    return;
  }

  const current = currentQuestion();
  const correct = state.selectedOption === current.answerIndex;
  const oldDifficulty = getDifficulty();
  const nextAction = applyAdaptiveRule(correct);
  const attempt = {
    id: `attempt-${Date.now()}-${Math.round(Math.random() * 1000)}`,
    studentName: state.studentName,
    grade: state.grade,
    topic: state.topic,
    difficulty: oldDifficulty,
    questionId: current.id,
    correct,
    selectedOption: state.selectedOption,
    createdAt: new Date().toISOString(),
    synced: false
  };

  state.attempts.push(attempt);
  await putRecord("attempts", attempt);
  await saveMeta();

  state.readyForNext = true;
  el.submitButton.innerHTML = "Siguiente";
  el.feedbackBox.textContent = feedbackText(correct, current, nextAction);
  el.feedbackBox.className = `feedback ${correct ? "is-good" : "is-bad"}`;
  renderStudent();
  renderReports();
}

function applyAdaptiveRule(correct) {
  const streak = getStreak();
  const currentDifficulty = getDifficulty();
  let action = "mantiene";

  if (correct) {
    streak.correct += 1;
    streak.wrong = 0;
    if (streak.correct >= 3) {
      const next = moveDifficulty(currentDifficulty, 1);
      setDifficulty(next);
      streak.correct = 0;
      action = next === currentDifficulty ? "domina avanzado" : "sube";
    }
  } else {
    streak.wrong += 1;
    streak.correct = 0;
    if (streak.wrong >= 2) {
      const next = moveDifficulty(currentDifficulty, -1);
      setDifficulty(next);
      streak.wrong = 0;
      action = next === currentDifficulty ? "refuerza" : "baja";
    }
  }

  return action;
}

function moveDifficulty(current, direction) {
  const index = DIFFICULTIES.indexOf(current);
  const nextIndex = Math.max(0, Math.min(DIFFICULTIES.length - 1, index + direction));
  return DIFFICULTIES[nextIndex];
}

function feedbackText(correct, questionItem, action) {
  if (correct && action === "sube") {
    return "Respuesta correcta. El motor adaptativo subió la dificultad para el siguiente ejercicio.";
  }
  if (correct && action === "domina avanzado") {
    return "Respuesta correcta. Mantienes nivel avanzado en este tema.";
  }
  if (!correct && action === "baja") {
    return `Respuesta incorrecta. Refuerzo: ${questionItem.hint}`;
  }
  if (!correct && action === "refuerza") {
    return `Respuesta incorrecta. Recomendación: repasar este tema antes de avanzar. ${questionItem.hint}`;
  }
  return correct ? "Respuesta correcta. Sigue acumulando aciertos." : `Respuesta incorrecta. ${questionItem.hint}`;
}

function currentQuestion() {
  return getAllQuestions().find((item) => item.id === state.currentQuestionId) || seedQuestions[0];
}

function renderAll() {
  renderAuthShell();
  el.studentName.value = state.studentName;
  el.gradeSelect.value = String(state.grade);
  el.topicSelect.value = state.topic;
  el.newQuestionGrade.value = String(state.grade);
  el.newQuestionTopic.value = state.topic;
  el.offlineToggle.checked = state.forceOffline;
  setAuthMode(state.authMode);
  if (state.rememberedLogin) {
    setAuthRole(state.rememberedLogin.role);
    el.authEmail.value = state.rememberedLogin.email;
    el.rememberPassword.checked = true;
  }
  renderAttemptStatus();
  if (state.session) {
    setRole(state.session.role);
  }
  updateConnectionStatus();
  renderStudent();
  renderReports();
  renderCustomQuestions();
}

function renderAuthShell() {
  const loggedIn = Boolean(state.session);
  el.authStage.hidden = loggedIn;
  el.appWorkspace.hidden = !loggedIn;
  el.roleTabs.hidden = true;
  el.teacherProfileMenu.hidden = !loggedIn || state.session.role !== "teacher";

  if (loggedIn && state.session.role === "teacher") {
    el.teacherProfileName.textContent = state.session.name || "Docente";
  }
}

function renderStudent() {
  const difficulty = getDifficulty();
  const questionItem = currentQuestion();
  const studentAttempts = ownAttempts();
  const correct = studentAttempts.filter((item) => item.correct).length;
  const accuracy = studentAttempts.length ? Math.round((correct / studentAttempts.length) * 100) : 0;
  const streak = getStreak();

  el.gradeBadge.textContent = `${state.grade}.° secundaria`;
  el.difficultyBadge.textContent = DIFFICULTY_LABELS[difficulty];
  el.topicBadge.textContent = state.topic;
  el.questionText.textContent = questionItem.text;
  el.scoreRing.style.setProperty("--score", `${accuracy}%`);
  el.accuracyValue.textContent = `${accuracy}%`;
  el.correctStreak.textContent = streak.correct;
  el.wrongStreak.textContent = streak.wrong;
  el.currentLevel.textContent = DIFFICULTY_LABELS[difficulty];
  el.recommendation.textContent = recommendationLabel(accuracy, studentAttempts.length, streak.wrong);
  el.localAnswerCount.textContent = state.attempts.length;
  el.syncedAnswerCount.textContent = state.attempts.filter((item) => item.synced).length;
  el.pendingCount.textContent = state.attempts.filter((item) => !item.synced).length;
  el.submitButton.innerHTML = state.readyForNext
    ? "Siguiente"
    : '<svg><use href="#icon-check"></use></svg>Comprobar';

  renderOptions(questionItem);
}

function renderOptions(questionItem) {
  el.optionsList.innerHTML = "";

  questionItem.options.forEach((option, index) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "option-button";
    button.setAttribute("role", "radio");
    button.setAttribute("aria-checked", String(state.selectedOption === index));

    if (state.selectedOption === index) {
      button.classList.add("is-selected");
    }

    if (state.readyForNext) {
      button.disabled = true;
      if (index === questionItem.answerIndex) {
        button.classList.add("is-correct");
      } else if (index === state.selectedOption) {
        button.classList.add("is-wrong");
      }
    }

    button.innerHTML = `<span class="option-letter">${String.fromCharCode(65 + index)}</span><span>${option}</span>`;
    button.addEventListener("click", () => {
      if (state.readyForNext) return;
      state.selectedOption = index;
      renderOptions(questionItem);
    });
    el.optionsList.append(button);
  });
}

function ownAttempts() {
  return state.attempts.filter((item) => item.studentName === state.studentName);
}

function recommendationLabel(accuracy, total, wrongStreak) {
  if (!total) return "Practicar";
  if (wrongStreak > 0 || accuracy < 55) return "Repasar";
  if (accuracy >= 85) return "Avanzar";
  return "Continuar";
}

function renderReports() {
  const rows = buildReportRows();
  const risky = rows.filter((row) => row.accuracy < 60).length;

  el.bankCount.textContent = `${getAllQuestions().length} preguntas`;
  el.riskCount.textContent = `${risky} ${risky === 1 ? "alerta" : "alertas"}`;
  el.riskCount.classList.toggle("success", risky === 0);

  renderTopicBars(rows);
  renderStudentTable(rows);
}

function buildReportRows() {
  const sampleAttempts = buildSampleAttempts();
  const actualAttempts = state.attempts.map((item) => ({
    studentName: item.studentName,
    grade: item.grade,
    topic: item.topic,
    correct: item.correct
  }));

  const all = [...sampleAttempts, ...actualAttempts];
  const registeredStudents = state.accounts
    .filter((account) => account.role === "student")
    .map((account) => account.name);
  const reportStudents = Array.from(new Set([...STUDENT_POOL, ...registeredStudents, state.studentName]));

  return reportStudents.map((student, index) => {
    const studentAttempts = all.filter((item) => item.studentName === student);
    const total = studentAttempts.length || 1;
    const correct = studentAttempts.filter((item) => item.correct).length;
    const topicStats = TOPICS.map((topic) => {
      const topicAttempts = studentAttempts.filter((item) => item.topic === topic);
      const topicAccuracy = topicAttempts.length
        ? Math.round((topicAttempts.filter((item) => item.correct).length / topicAttempts.length) * 100)
        : 100;
      return { topic, accuracy: topicAccuracy };
    }).sort((a, b) => a.accuracy - b.accuracy);

    return {
      student,
      grade: index === 0 ? state.grade : (index % 5) + 1,
      accuracy: Math.round((correct / total) * 100),
      weakTopic: topicStats[0]?.topic || TOPICS[0]
    };
  }).sort((a, b) => a.accuracy - b.accuracy);
}

function buildSampleAttempts() {
  const attempts = [];
  STUDENT_POOL.slice(1).forEach((student, studentIndex) => {
    TOPICS.forEach((topic, topicIndex) => {
      for (let round = 0; round < 4; round += 1) {
        const scoreSeed = studentIndex * 9 + topicIndex * 5 + round;
        attempts.push({
          studentName: student,
          grade: (studentIndex % 5) + 1,
          topic,
          correct: scoreSeed % (studentIndex + 3) !== 0
        });
      }
    });
  });
  return attempts;
}

function renderTopicBars(rows) {
  el.topicBars.innerHTML = "";

  TOPICS.forEach((topic) => {
    const relatedAttempts = [...buildSampleAttempts(), ...state.attempts]
      .filter((item) => item.topic === topic);
    const total = relatedAttempts.length || 1;
    const accuracy = Math.round((relatedAttempts.filter((item) => item.correct).length / total) * 100);
    const row = document.createElement("div");
    const levelClass = accuracy < 60 ? "low" : accuracy < 80 ? "mid" : "";
    row.className = "bar-row";
    row.innerHTML = `
      <span>${topic}</span>
      <div class="bar-track"><div class="bar-fill ${levelClass}" style="width: ${accuracy}%"></div></div>
      <strong>${accuracy}%</strong>
    `;
    el.topicBars.append(row);
  });
}

function renderStudentTable(rows) {
  el.studentReportBody.innerHTML = "";

  rows.forEach((row) => {
    const tr = document.createElement("tr");
    const status = statusFor(row.accuracy);
    tr.innerHTML = `
      <td>${row.student}</td>
      <td>${row.grade}.°</td>
      <td>${row.accuracy}%</td>
      <td>${row.weakTopic}</td>
      <td><span class="status-label ${status.className}">${status.label}</span></td>
    `;
    el.studentReportBody.append(tr);
  });
}

function statusFor(accuracy) {
  if (accuracy < 60) return { label: "Necesita refuerzo", className: "low" };
  if (accuracy < 80) return { label: "En proceso", className: "mid" };
  return { label: "Avanza bien", className: "good" };
}

async function saveCustomQuestion(event) {
  event.preventDefault();
  const options = el.newOptionInputs.map((input) => input.value.trim());
  const text = el.newQuestionText.value.trim();

  if (!text || options.some((option) => !option)) {
    el.formMessage.textContent = "Completa la pregunta y sus cuatro alternativas.";
    return;
  }

  const newQuestion = {
    id: `custom-${Date.now()}`,
    grade: Number(el.newQuestionGrade.value),
    topic: el.newQuestionTopic.value,
    difficulty: el.newQuestionDifficulty.value,
    text,
    options,
    answerIndex: Number(el.newQuestionAnswer.value),
    hint: el.newQuestionHint.value.trim() || "Revisa el procedimiento paso a paso."
  };

  state.customQuestions.push(newQuestion);
  await putRecord("customQuestions", newQuestion);
  el.questionForm.reset();
  el.newQuestionGrade.value = String(state.grade);
  el.newQuestionTopic.value = state.topic;
  el.formMessage.textContent = "Pregunta guardada en el banco local.";
  renderReports();
  renderCustomQuestions();
}

function renderCustomQuestions() {
  el.customCount.textContent = `${state.customQuestions.length} nuevas`;
  el.recentQuestions.innerHTML = "";

  if (!state.customQuestions.length) {
    const empty = document.createElement("div");
    empty.className = "recent-item";
    empty.innerHTML = "<strong>Sin preguntas nuevas</strong><span>El banco base ya está cargado para todos los grados.</span>";
    el.recentQuestions.append(empty);
    return;
  }

  state.customQuestions.slice(-3).reverse().forEach((item) => {
    const div = document.createElement("div");
    div.className = "recent-item";
    div.innerHTML = `
      <strong>${item.text}</strong>
      <span>${item.grade}.° secundaria</span>
      <span>${item.topic}</span>
      <span>${DIFFICULTY_LABELS[item.difficulty]}</span>
    `;
    el.recentQuestions.append(div);
  });
}

async function syncPendingAttempts() {
  if (!isOnline()) {
    el.feedbackBox.textContent = "No hay conexión. Las respuestas seguirán guardadas en este dispositivo.";
    el.feedbackBox.className = "feedback is-bad";
    return;
  }

  const pending = state.attempts.filter((item) => !item.synced);
  await Promise.all(pending.map(async (attempt) => {
    attempt.synced = true;
    await putRecord("attempts", attempt);
  }));

  el.feedbackBox.textContent = pending.length
    ? "Progreso sincronizado con el servidor simulado."
    : "No hay respuestas pendientes por sincronizar.";
  el.feedbackBox.className = "feedback is-good";
  renderStudent();
  renderReports();
}

function isOnline() {
  return navigator.onLine && !state.forceOffline;
}

function updateConnectionStatus() {
  const online = isOnline();
  el.connectionStatus.classList.toggle("is-offline", !online);
  el.connectionStatus.innerHTML = `<svg><use href="#icon-wifi"></use></svg>${online ? "En línea" : "Sin internet"}`;
  el.syncButton.disabled = !online;
}

async function openDatabase() {
  if (!("indexedDB" in window)) {
    return null;
  }

  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const database = request.result;
      if (!database.objectStoreNames.contains("attempts")) {
        database.createObjectStore("attempts", { keyPath: "id" });
      }
      if (!database.objectStoreNames.contains("customQuestions")) {
        database.createObjectStore("customQuestions", { keyPath: "id" });
      }
      if (!database.objectStoreNames.contains("meta")) {
        database.createObjectStore("meta", { keyPath: "key" });
      }
      if (!database.objectStoreNames.contains("accounts")) {
        database.createObjectStore("accounts", { keyPath: "id" });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function loadStoredState() {
  const [attempts, customQuestions, accounts, meta] = await Promise.all([
    getAllRecords("attempts"),
    getAllRecords("customQuestions"),
    getAllRecords("accounts"),
    getRecord("meta", "app-state")
  ]);

  state.attempts = attempts || [];
  state.customQuestions = customQuestions || [];
  state.accounts = accounts || [];

  if (meta?.value) {
    state.studentName = meta.value.studentName || state.studentName;
    state.grade = meta.value.grade || state.grade;
    state.topic = meta.value.topic || state.topic;
    state.levels = meta.value.levels || {};
    state.streaks = meta.value.streaks || {};
    state.forceOffline = Boolean(meta.value.forceOffline);
    state.session = meta.value.session || null;
    state.persistSession = Boolean(meta.value.persistSession);
    state.rememberedLogin = meta.value.rememberedLogin || null;
    state.loginFailures = meta.value.loginFailures || {};
    state.lockouts = meta.value.lockouts || {};
    if (state.session) {
      state.role = state.session.role;
    }
  }
}

async function ensureDemoAccounts() {
  for (const account of DEMO_ACCOUNTS) {
    if (!state.accounts.some((item) => item.id === account.id)) {
      state.accounts.push(account);
      await putRecord("accounts", account);
    }
  }
}

async function saveMeta() {
  await putRecord("meta", {
    key: "app-state",
    value: {
      studentName: state.studentName,
      grade: state.grade,
      topic: state.topic,
      levels: state.levels,
      streaks: state.streaks,
      forceOffline: state.forceOffline,
      session: state.persistSession ? state.session : null,
      persistSession: state.persistSession,
      rememberedLogin: state.rememberedLogin,
      loginFailures: state.loginFailures,
      lockouts: state.lockouts
    }
  });
}

function withStore(storeName, mode, callback) {
  if (!db) return Promise.resolve(null);

  return new Promise((resolve, reject) => {
    const transaction = db.transaction(storeName, mode);
    const store = transaction.objectStore(storeName);
    const request = callback(store);

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

function getAllRecords(storeName) {
  return withStore(storeName, "readonly", (store) => store.getAll());
}

function getRecord(storeName, key) {
  return withStore(storeName, "readonly", (store) => store.get(key));
}

function putRecord(storeName, value) {
  return withStore(storeName, "readwrite", (store) => store.put(value));
}

function registerServiceWorker() {
  if (!("serviceWorker" in navigator)) return;

  navigator.serviceWorker.register("sw.js").catch(() => {
    el.connectionStatus.classList.add("is-offline");
  });
}
