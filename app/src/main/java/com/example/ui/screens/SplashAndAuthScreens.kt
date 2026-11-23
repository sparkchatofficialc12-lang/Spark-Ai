package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.SparkCard
import com.example.ui.components.SparkGoldButton
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.SecondaryIce
import com.example.ui.theme.SoftGold
import com.example.ui.theme.SparkBrushes
import com.example.ui.theme.SteelBorder

@Composable
fun SplashScreen(statusLabel: String = "Preparing your cinema studio...") {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MidnightBlack)
                    .border(2.dp, ChampagneGold, RoundedCornerShape(32.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_spark_logo),
                    contentDescription = "Spark AI Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "SPARK AI",
                style = MaterialTheme.typography.displayLarge,
                color = ChampagneGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Imagine It. Create It. Watch It.",
                style = MaterialTheme.typography.titleMedium,
                color = SoftGold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            LinearProgressIndicator(
                color = ChampagneGold,
                trackColor = SteelBorder,
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryIce,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AuthScreen(
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SparkBrushes.CosmicBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MidnightBlack)
                    .border(2.dp, ChampagneGold, RoundedCornerShape(28.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_spark_logo),
                    contentDescription = "Spark AI Studio Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(22.dp))
            Text(
                text = "SPARK AI",
                style = MaterialTheme.typography.displayMedium,
                color = ChampagneGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Imagine It. Create It. Watch It.",
                style = MaterialTheme.typography.titleMedium,
                color = SoftGold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            SparkCard(borderColor = ChampagneGold.copy(alpha = 0.7f)) {
                Text(
                    text = "AI Movie Creation & Social Video Studio",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Founded by Christian Okafor (Sparkling). Sign in securely with Google to create scripts, storyboards, movies, music videos, singing portraits, and share with the Spark AI community.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryIce
                )
                Spacer(Modifier.height(16.dp))
                FeatureLine(Icons.Default.Movie, "Text-to-Movie, Image-to-Movie, Script-to-Movie & Remix Templates")
                Spacer(Modifier.height(8.dp))
                FeatureLine(Icons.Default.AutoAwesome, "Gemini Screenplays, Storyboards, Nano Banana Art & Voice TTS")
                Spacer(Modifier.height(8.dp))
                FeatureLine(Icons.Default.Security, "Zero-trust Firebase Authentication & Encrypted Cloud Projects")
                Spacer(Modifier.height(20.dp))

                if (!authError.isNullOrBlank()) {
                    Surface(
                        color = ErrorRed.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, ErrorRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (isSigningIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = ChampagneGold, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Verifying Google account...", color = SoftGold)
                    }
                } else {
                    SparkGoldButton(
                        text = "Continue with Google",
                        onClick = onGoogleSignInClick,
                        icon = Icons.Default.AccountCircle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Includes 20 Free Starter Credits • Unlimited Gemini Screenplay Planning",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryIce,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
