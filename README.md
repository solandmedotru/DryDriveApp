# DryDrive 🚗☀️

**DryDrive** (ранее WashWait/MeteoWash) - это Android-приложение, которое помогает автовладельцам выбрать оптимальный день для мойки автомобиля, основываясь на прогнозе погоды. Приложение также стремится предоставлять полезные рекомендации и, в перспективе, информацию о ближайших автомойках.

<p align="center">
  <img src="https://github.com/solandmedotru/DryDriveApp/blob/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" alt="DryDrive App Icon" width="150"/>
  <!-- TODO: Замените path/to/your/app_icon.png на реальный путь к иконке вашего приложения в репозитории -->
  <!-- Или загрузите скриншоты: -->
</p>

## 🌟 Описание

Устали мыть машину только для того, чтобы на следующий день пошел дождь? DryDrive анализирует прогноз погоды и дает рекомендации, когда лучше всего посетить автомойку, а когда стоит воздержаться, чтобы ваши деньги и усилия не пропали даром.

Просто выберите ваш город, и приложение покажет:
*   Текущую погоду и детальный прогноз.
*   Рекомендации, основанные на погодных условиях (например, взять зонт, пить больше воды, проверить шины).
*   Советы по мойке автомобиля.

В будущем планируется интеграция с картами для поиска ближайших автомоек, отображения их контактной информации и рейтинга.

Наша цель - помочь заботливым водителям экономить время и деньги, избегая мойки автомобиля перед неблагоприятными погодными условиями. Помните, что прогноз погоды не всегда точен на 100%, поэтому относитесь к советам приложения с пониманием.

<p align="center">
 <img src="https://github.com/solandmedotru/DryDriveApp/blob/main/Screenshot_20250916_180411.png" alt="Screenshot 1" width="200"/> 
</p>


## ✨ Ключевые возможности (Текущие и Планируемые)

*   **Прогноз погоды**: Отображение текущей погоды и прогноза на несколько дней вперед.
*   **Выбор города**: Поиск и выбор города для получения релевантного прогноза.
*   **Рекомендации по погоде**: Советы, связанные с текущими погодными условиями.
*   **Советы по мойке**: Индикация благоприятных и неблагоприятных дней для мойки автомобиля.
*   **Многоязычность**: Поддержка нескольких языков (текущая реализация: русский, английский, системный).
*   **Динамическая тема**: Поддержка светлой и темной тем оформления.
*   **(В планах)** Интеграция с картами для поиска автомоек.
*   **(В планах)** Отображение информации об автомойках (адрес, телефон, рейтинг).

## 🛠️ Технологический стек

Проект разработан с использованием современных технологий и практик Android-разработки:

*   **Язык программирования**: [Kotlin](https://kotlinlang.org/) (включая Coroutines и Flow для асинхронных операций).
*   **Архитектура**: MVVM (Model-View-ViewModel).
*   **Пользовательский интерфейс**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (декларативный UI toolkit).
*   **Компоненты Jetpack**:
    *   **ViewModel**: Для управления UI-related данными и бизнес-логикой.
    *   **LiveData/StateFlow**: Для предоставления наблюдаемых данных из ViewModel в UI. (Мы активно использовали `StateFlow`).
    *   **Navigation Compose**: Для навигации между экранами.
    *   **Room**: Для локального хранения данных (например, пользовательских предпочтений или кешированных данных, если будет реализовано). (У нас используется **DataStore Preferences** для пользовательских настроек).
    *   **Lifecycle**: Для управления жизненным циклом компонентов.
    *   **Hilt**: Для внедрения зависимостей.
*   **Работа с сетью**:
    *   [Retrofit](https://square.github.io/retrofit/) - для выполнения HTTP-запросов к API.
    *   [OkHttp](https://square.github.io/okhttp/) - как HTTP-клиент для Retrofit.
    *   [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - для парсинга JSON.
*   **Хранение данных**:
    *   [Jetpack DataStore (Preferences)](https://developer.android.com/topic/libraries/architecture/datastore) - для хранения простых пользовательских настроек (выбранный город, язык).
*   **Загрузка изображений**:
    *   [Coil](https://coil-kt.github.io/coil/) (если используется для загрузки иконок погоды из сети).
*   **API погоды**: [OpenWeatherMap API](https://openweathermap.org/api) (или аналогичный, который вы используете).
*   **Локализация**: Поддержка нескольких языков через строковые ресурсы Android.

## ⚙️ Требования к сборке

*   **Android Studio**: Iguana | 2023.2.1 Patch 1 или новее (рекомендуется последняя стабильная версия).
*   **Gradle**: Версия, совместимая с используемым Android Gradle Plugin.
*   **`minSdkVersion`**: `24`
*   **`compileSdkVersion` / `targetSdkVersion`**: `36`

## 🚀 Contributing

Contributions are always welcome!
Follow the "fork-and-pull" Git workflow.

1. **Fork** the repo on GitHub
2. **Clone** the project to your own machine
3. **Commit** changes to your own branch
4. **Merge** with current *development* branch
5. **Push** your work back up to your fork
6. Submit a **Pull request** your changes can be reviewed

**NOTE:**
Prevent code-style related changes (at least run ⌘+O, ⌘+L) before commiting.

### License

	Copyright © 2019 Solomin Andrey

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
	implied.
	See the License for the specific language governing permissions and
	limitations under the License.