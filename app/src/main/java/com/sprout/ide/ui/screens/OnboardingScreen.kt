package com.sprout.ide.ui.screens

import android.app.Activity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sprout.ide.AppViewModel
import com.sprout.ide.R
import com.sprout.ide.cloud.Cloud
import kotlinx.coroutines.launch

private class Page(val title: String, val text: String, val icon: ImageVector?, val shape: @Composable () -> Shape)

@Composable
fun OnboardingScreen(vm: AppViewModel) {
    val pages = listOf(
        Page("Sprout IDE", "Полноценная среда для кода прямо в телефоне. Пиши, запускай и делись проектами где угодно.",
            null) { MaterialShapes.Cookie9Sided.toShape() },
        Page("24 языка", "Kotlin, Python, JS, C++, Go, Rust и другие: подсветка синтаксиса, автоотступы, поиск с заменой, вкладки, предпросмотр HTML и консоль JavaScript.",
            Icons.Rounded.Code) { MaterialShapes.Sunny.toShape() },
        Page("Настрой под себя", "Цвета, формы, шрифты, схемы подсветки, панель символов — меняется почти всё.",
            Icons.Rounded.Palette) { MaterialShapes.Flower.toShape() },
        Page("Профиль и сообщество", "Создай аккаунт, оформи профиль с баннером, градиентами и бейджами и выкладывай свои проекты с открытым кодом.",
            Icons.Rounded.Public) { MaterialShapes.Clover4Leaf.toShape() },
    )
    val total = pages.size + 1
    val pager = rememberPagerState { total }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).statusBarsPadding().navigationBarsPadding()) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { i ->
            if (i < pages.size) IntroPage(pages[i]) else ChoicePage(vm)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                repeat(total) { i ->
                    val selected = pager.currentPage == i
                    val w by animateDpAsState(if (selected) 28.dp else 10.dp, label = "dot")
                    Box(
                        Modifier.height(10.dp).width(w).clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            if (pager.currentPage < pages.size) {
                TextButton(onClick = { scope.launch { pager.animateScrollToPage(pages.size) } }) { Text("Пропустить") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text("Далее")
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun IntroPage(p: Page) {
    val spin = rememberInfiniteTransition(label = "spin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart), label = "a")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp).padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).rotate(angle).clip(p.shape()).background(MaterialTheme.colorScheme.primaryContainer))
            if (p.icon != null) {
                Icon(p.icon, null, Modifier.size(96.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Image(painterResource(R.drawable.ic_logo), null, Modifier.size(150.dp))
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(p.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(p.text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ChoicePage(vm: AppViewModel) {
    val activity = LocalContext.current as Activity
    var error by remember { mutableStateOf<String?>(null) }
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Как продолжить?", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        ChoiceCard(
            title = "Аккаунт Google",
            features = listOf(true to "Проекты и файлы", true to "Профиль: баннер, градиенты, бейджи", true to "Публикация open-source проектов", true to "Сообщество и клонирование чужих проектов"),
            container = MaterialTheme.colorScheme.primaryContainer,
            content = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Button(
                onClick = { error = null; vm.signIn(activity) { error = it } },
                enabled = Cloud.configured && !vm.signingIn,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (vm.signingIn) LoadingIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
                else {
                    Text("G", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.width(10.dp))
                    Text("Войти через Google", style = MaterialTheme.typography.titleMedium)
                }
            }
            if (!Cloud.configured) {
                Spacer(Modifier.height(8.dp))
                Text("Вход появится, когда приложение подключат к серверу.", style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(12.dp))
        ChoiceCard(
            title = "Гость",
            features = listOf(true to "Проекты и файлы на телефоне", false to "Без профиля и публикаций", false to "Без сообщества"),
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            content = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            OutlinedButton(
                onClick = { vm.finishOnboarding() },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Rounded.Person, null)
                Spacer(Modifier.width(8.dp))
                Text("Продолжить как гость")
            }
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    features: List<Pair<Boolean, String>>,
    container: Color,
    content: Color,
    shape: Shape,
    action: @Composable () -> Unit,
) {
    Surface(color = container, contentColor = content, shape = shape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            features.forEach { (ok, text) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Icon(if (ok) Icons.Rounded.Check else Icons.Rounded.Close, null, Modifier.size(18.dp),
                        tint = if (ok) content else content.copy(alpha = 0.5f))
                    Spacer(Modifier.width(10.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (ok) content else content.copy(alpha = 0.6f))
                }
            }
            Spacer(Modifier.height(16.dp))
            action()
        }
    }
}
