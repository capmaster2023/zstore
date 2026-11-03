package com.graceofgod.zstore.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.graceofgod.zstore.BuildConfig
import com.graceofgod.zstore.ui.theme.Z
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(deps: Deps) {
    val scope = rememberCoroutineScope()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 28.dp, bottom = 24.dp)
    ) {
        Text("Compte", style = MaterialTheme.typography.displaySmall)
        val u = deps.user
        if (u == null) {
            Text(
                "Connecte-toi pour acheter des apps et les retrouver sur tous tes appareils.",
                color = Z.Muted, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )
            AuthForm(deps, onDone = {})
        } else {
            Surface(color = Z.Surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.padding(top = 18.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(u.name ?: u.email.substringBefore('@'), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        if (u.isAdmin) Pill("Admin", Z.Lime, filled = true)
                    }
                    Text(u.email, color = Z.Muted)
                }
            }
            OutlinedButton(
                onClick = { scope.launch { deps.repo.logout() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) { Text("Se déconnecter") }
        }
        SectionTitle("À propos")
        Text("Z-Store ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = Z.Muted)
        Text(BuildConfig.API_BASE, color = Z.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AuthForm(deps: Deps, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column {
        if (register) {
            OutlinedTextField(
                value = name, onValueChange = { name = it }, label = { Text("Nom") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Mot de passe") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            supportingText = { if (register) Text("8 caractères minimum") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        error?.let { Text(it, color = Z.Red, modifier = Modifier.padding(top = 8.dp)) }
        Button(
            enabled = !busy && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                busy = true
                error = null
                scope.launch {
                    val r = if (register) deps.repo.register(email, password, name) else deps.repo.login(email, password)
                    busy = false
                    r.onSuccess { onDone() }.onFailure { error = it.message }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .height(50.dp),
        ) {
            if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            else Text(if (register) "Créer mon compte" else "Se connecter")
        }
        TextButton(onClick = { register = !register; error = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (register) "J'ai déjà un compte" else "Créer un compte")
        }
    }
}
