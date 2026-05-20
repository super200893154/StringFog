
# StringFog (Fork - AGP 9.x Compatible)

一款自动对 dex/aar/jar 文件中的字符串进行加密的 Android 插件工具，正如名字所言，给字符串加上一层雾霭，使人难以窥视其真面目。

- 支持 Java/Kotlin。
- 支持 app 打包生成的 apk 加密。
- 支持 aar 和 jar 等库文件加密。
- 支持加解密算法的自主扩展。
- 支持配置可选代码加密。
- 完全 Gradle 自动化集成。
- 不支持 InstantRun。

> 本仓库是基于 [MegatronKing/StringFog](https://github.com/MegatronKing/StringFog) 的 fork 版本，适配了 **AGP 9.x (android.newDsl=true)** 和 **Gradle 9.x** 的最新 API 变更。

### 原理

![](https://github.com/MegatronKing/StringFog/blob/master/assets/flow.png)<br>

- 加密前：
```java
String a = "This is a string!";
```

- 加密后：
```java
String a = StringFog.decrypt(new byte[]{-113, 71...}, new byte[]{-23, 53});
```

- 运行时：
```java
decrypt: new byte[]{-113, 71...} => "This is a string!"
```

### 混淆

StringFog 和混淆完全不冲突，也不需要配置反混淆，实际上 StringFog 配上混淆效果会更好！

### 使用

本 fork 版本通过 **JitPack** 发布，使用前请先确保项目中配置了 JitPack 仓库。

##### 1、在根目录 build.gradle 中引入插件依赖。

```groovy
buildscript {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }   // 必须添加 JitPack 仓库
    }
    dependencies {
        ...
        classpath 'com.github.super200893154.StringFog:gradle-plugin:5.3.1'
        // 选用加解密算法库，默认实现了 xor 算法，也可以使用自己的加解密库。
        classpath 'com.github.super200893154.StringFog:xor:5.3.1'
    }
}
```

> **注意**：JitPack 会将仓库所有模块以**同一个 tag 版本号**发布。因此 `gradle-plugin`、`xor` 等模块的版本号必须保持一致。

##### 2、在 app 或 lib 的 build.gradle 中配置插件。

```groovy
apply plugin: 'stringfog'

// 导入 RandomKeyGenerator 类，如果使用 HardCodeKeyGenerator，更换下类名
import com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator
import com.github.megatronking.stringfog.plugin.StringFogMode

stringfog {
    // 必要：加解密库的实现类路径，需和上面配置的加解密算法库一致。
    implementation 'com.github.megatronking.stringfog.xor.StringFogImpl'
    // 可选：StringFog 会自动尝试获取 packageName，如果遇到获取失败的情况，可以显式地指定。
    packageName 'com.your.package.name'
    // 可选：加密开关，默认开启。
    enable true
    // 可选：指定需加密的代码包路径，可配置多个，未指定将默认全部加密。
    fogPackages = ['com.xxx.xxx']
    // 可选：指定密钥生成器，默认使用长度8的随机密钥（每个字符串均有不同随机密钥），
    // 也可以指定一个固定的密钥：HardCodeKeyGenerator("This is a key")
    kg new RandomKeyGenerator()
    // 可选：用于控制字符串加密后在字节码中的存在形式，默认为 base64，
    // 也可以使用 text 或者 bytes
    mode StringFogMode.base64
}
```

KTS 中配置参考：

```kotlin
plugins {
    // ...lib or application
    id("stringfog")
}

configure<com.github.megatronking.stringfog.plugin.StringFogExtension> {
    // 必要：加解密库的实现类路径，需和上面配置的加解密算法库一致。
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    // 可选：加密开关，默认开启。
    enable = true
    // 可选：指定需加密的代码包路径，可配置多个，未指定将默认全部加密。
    // fogPackages = arrayOf("com.xxx.xxx")
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    // base64 或者 bytes
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}
```

##### 3、在 app 或 lib 的 build.gradle 中引入加解密库依赖（运行时解密用）。

```groovy
dependencies {
    ...
    // 这里要和上面选用的加解密算法库一致，用于运行时解密。
    implementation 'com.github.super200893154.StringFog:xor:5.3.1'
}
```

### AGP 9.x (android.newDsl=true) 集成指南

> **本插件的 v5.3.1 版本已适配 AGP 9.x 的新 DSL 和 Variant API。但消费端项目如果启用了 `android.newDsl=true`，还需要额外配置以下内容，否则会编译失败。**

以下是在使用本插件 + AGP 9.x + `android.newDsl=true` 时可能遇到的问题及解决方案。

#### 问题 1：Kotlin Android 插件冲突

**错误信息：**
```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

**原因：** AGP 9.x 内置了 Kotlin 支持（`android.builtInKotlin`），如果项目中**同时**显式声明了 `kotlin.android` 插件，会导致 `kotlin` 扩展被重复注册。

**解决：**

1. 在 `gradle.properties` 中启用 AGP 内置 Kotlin 支持：
```properties
android.builtInKotlin=true
android.disallowKotlinSourceSets=false
```

2. **移除**所有模块 `build.gradle.kts` 中的 `kotlin.android` 插件声明：
```kotlin
// ❌ 移除：
plugins {
    alias(libs.plugins.kotlin.android)
}
```

3. **同步移除**根 `build.gradle.kts` 中的 `apply false` 声明：
```kotlin
// ❌ 移除：
alias(libs.plugins.kotlin.android) apply false
```

#### 问题 2：kotlinOptions 废弃

**错误信息：**
```
'kotlinOptions' is deprecated
```

**原因：** 启用 `android.builtInKotlin=true` 后，旧的 `kotlinOptions {}` 块不再可用。

**解决：** 将 `kotlinOptions` 替换为 `kotlin { compilerOptions {} }`：

```kotlin
// ❌ 旧的写法：
android {
    kotlinOptions {
        jvmTarget = "17"
    }
}

// ✅ 新的写法（放在 android 块外部或内部均可）：
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

#### 问题 3：composeOptions 冲突

**错误信息：**
```
Could not get unknown property 'kotlinCompilerExtensionVersion'
```

**原因：** AGP 9.x 新 DSL 下，旧的 `composeOptions { kotlinCompilerExtensionVersion }` 配置方式不再兼容。

**解决：** 移除 `composeOptions` 块，改用 Compose Compiler Gradle 插件方式声明。

```kotlin
// ❌ 移除：
android {
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

// ✅ 改为使用 Compose Compiler Gradle 插件（如果使用 Compose）：
plugins {
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}
```

#### 问题 4：maven-publish 找不到 release 组件

**错误信息：**
```
SoftwareComponent with name 'release' not found.
```

**原因：** AGP 9.x 需要显式声明要发布的 Android library variant。

**解决：** 在 library 模块的 `android` 块中添加：
```kotlin
android {
    publishing {
        singleVariant("release")
    }
}
```

#### 问题 5：依赖冲突（旧版 StringFog interface）

**错误信息：**
```
Duplicate class com.github.megatronking.stringfog.Base64 found in modules ...
```

**原因：** 如果项目中引用了第三方库（如自定义加解密库），该库仍然依赖旧版 Maven 坐标 `com.github.megatronking.stringfog:interface`，导致 class 冲突。

**解决：** 在引用该依赖时 `exclude` 旧版 StringFog interface：
```kotlin
implementation("com.example:custom-lib:1.0.0") {
    exclude(group = "com.github.megatronking.stringfog", module = "interface")
}
```

或者将自定义加解密库升级，使其依赖本 fork 的 interface：
```kotlin
implementation("com.github.super200893154.StringFog:interface:5.3.1")
```

#### 完整的 gradle.properties 配置参考

```properties
# AGP 9.x new DSL
android.newDsl=true

# AGP 内置 Kotlin 支持（必须）
android.builtInKotlin=true

# 允许使用旧版 Kotlin sourceSet 配置
android.disallowKotlinSourceSets=false

# AGP 9.x 需要
android.useAndroidX=true

# BuildConfig（StringFog 依赖）
android.defaults.buildfeatures.buildconfig=true
```

#### 完整的 app/build.gradle.kts 参考

```kotlin
plugins {
    id("com.android.application")
    // 注意：不要添加 id("org.jetbrains.kotlin.android")
    id("stringfog")
}

android {
    namespace = "com.your.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.your.app"
        minSdk = 26
        targetSdk = 35
    }

    buildFeatures {
        buildConfig = true   // StringFog 依赖 BuildConfig
    }

    // 不要使用 kotlinOptions，改用下面的方式：
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation 'com.github.super200893154.StringFog:xor:5.3.1'
}
```

#### 支持的 AGP 版本

- **AGP 8.x**：完全兼容，无需额外配置。
- **AGP 9.x (android.newDsl=true)**：兼容（需按上述指南配置）。
- **最低 Gradle 版本**：8.0
- **最低 JDK 版本**：11

#### AGP 9.x 集成清单（快速检查）

| 配置项 | 说明 | 必须 |
|--------|------|------|
| `android.builtInKotlin=true` | 启用内置Kotlin | ✅ |
| `android.disallowKotlinSourceSets=false` | 允许旧版sourceSet | ✅ |
| 移除 `kotlin.android` 插件 | 所有模块及根build.gradle | ✅ |
| `kotlinOptions` → `kotlin { compilerOptions {} }` | 替换所有模块 | ✅ |
| `buildFeatures { buildConfig = true }` | StringFog依赖 | ✅ |
| `jitpack.io` 仓库 | 添加至buildscript和dependencyResolution | ✅ |
| 依赖版本统一为 tag 版本 | 所有 StringFog 模块版本号一致 | ✅ |

### 注意事项

从 AGP 8.0 开始，默认不生成 BuildConfig，但是 StringFog 依赖此配置，请注意加上：

```kotlin
android {
    buildFeatures {
        buildConfig = true
    }
}
```

### 扩展

#### 注解反加密

如果开发者有不需要自动加密的类，可以使用注解 `@StringFogIgnore` 来忽略：

```java
@StringFogIgnore
public class Test {
    ...
}
```

#### 自定义加解密算法实现

实现 `IStringFog` 接口，参考 `stringfog-ext` 目录下面的 xor 算法实现。
注意某些算法在不同平台上会有差异，可能出现在运行时无法正确解密的问题。

```java
public final class StringFogImpl implements IStringFog {

    @Override
    public byte[] encrypt(String data, byte[] key) {
        // 自定义加密
    }

    @Override
    public String decrypt(byte[] data, byte[] key) {
        // 自定义解密
    }

    @Override
    public boolean shouldFog(String data) {
        // 控制指定字符串是否加密
        return true;
    }

}
```

自定义加解密库的 module 需要依赖本 fork 的 interface：

```kotlin
dependencies {
    implementation("com.github.super200893154.StringFog:interface:5.3.1")
}
```

#### 自定义密钥生成器

实现 `IKeyGenerator` 接口，参考 `RandomKeyGenerator` 的实现。

### 通过 JitPack 发布

本插件通过 [JitPack](https://jitpack.io) 发布，每次发布只需：

1. 更新版本号：
   - `stringfog-gradle-plugin/gradle.properties` 中的 `VERSION` 属性。

2. 创建 tag 并推送：
```bash
git tag v<新版本号>
git push origin v<新版本号>
```

3. JitPack 会自动构建。可在 [JitPack Build Status](https://jitpack.io/#super200893154/StringFog) 查看进度。

> JitPack 会将仓库中**所有模块**以同一个 tag 版本号发布，无需单独发布每个模块。

### 更新日志

#### v5.3.1

- **适配 AGP 9.x (android.newDsl=true)**：
  - 将 `BaseExtension` 替换为 `CommonExtension`，兼容 AGP 9.x 新 DSL。
  - 支持 `AndroidComponentsExtension` 新 Variant API。
- **适配 Gradle 9.x**：使用兼容的 Task 注册和 SourceDirectorySet API。
- **发布方式**：从 MavenCentral 迁移至 JitPack。
- 详细集成指南见上方「AGP 9.x 集成指南」章节。

#### v5.3.0（跳过的版本，JitPack 缓存问题）

#### v5.2.0

- 从 ASM7 升级到 ASM9。
- 修复多模块配置问题。

#### v5.1.0

- 修复获取无法获取 packageName 的问题。
- 修复无法指定 KeyGenerator 的问题。
- 优化生成 StringFog.java 文件的任务逻辑。
- 暂时移除 Mapping 文件生成逻辑，可能导致无法删除的问题。

#### v5.0.0

- 支持 Gradle 8.0。

#### v4.0.1

- 修复 Base64 API 版本兼容问题。

#### v4.0.0

- 使用 ASM7 以支持 Android 12。
- 支持 AGP 7.x 版本。
- 新增 `StringFogMode` 选项：base64 和 bytes。

#### v3.0.0 及更早版本

见原始仓库 [MegatronKing/StringFog](https://github.com/MegatronKing/StringFog) 的更新日志。

--------

    Copyright (C) 2016-2023, Megatron King

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
