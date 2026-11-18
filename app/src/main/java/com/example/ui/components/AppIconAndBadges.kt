package com.example.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.data.model.WakeMode

@Composable
fun AppIconImage(
  drawable: Drawable?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  val bitmap: ImageBitmap? =
    remember(drawable) {
      runCatching { drawable?.toBitmap(width = 112, height = 112)?.asImageBitmap() }.getOrNull()
    }

  if (bitmap != null) {
    Image(
      bitmap = bitmap,
      contentDescription = contentDescription,
      modifier = modifier.clip(RoundedCornerShape(14.dp)),
    )
  } else {
    Box(
      modifier =
        modifier
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Apps,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
      )
    }
  }
}

@Composable
fun WakeModeBadge(wakeMode: WakeMode, modifier: Modifier = Modifier) {
  val isBright = wakeMode == WakeMode.BRIGHT
  val bg =
    if (isBright) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
  val fg =
    if (isBright) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

  Surface(
    color = bg,
    contentColor = fg,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
      Icon(
        imageVector =
          if (isBright) Icons.Rounded.BrightnessHigh else Icons.Rounded.BrightnessLow,
        contentDescription = null,
        modifier = Modifier.size(13.dp),
      )
      Text(
        text =
          stringResource(if (isBright) R.string.wake_mode_bright else R.string.wake_mode_dim),
        style = MaterialTheme.typography.labelMedium,
      )
    }
  }
}

@Composable
fun DurationBadge(maxDurationMinutes: Int, modifier: Modifier = Modifier) {
  val label =
    if (maxDurationMinutes <= 0) stringResource(R.string.timeout_unlimited)
    else stringResource(R.string.timeout_minutes, maxDurationMinutes)

  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.Timer,
        contentDescription = null,
        modifier = Modifier.size(12.dp),
      )
      Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
  }
}

@Composable
fun StatusPulseDot(
  isAnimating: Boolean,
  color: Color,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val scale by
    infiniteTransition.animateFloat(
      initialValue = 1f,
      targetValue = if (isAnimating) 1.9f else 1f,
      animationSpec =
        infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Restart),
      label = "pulse_scale",
    )
  val alpha by
    infiniteTransition.animateFloat(
      initialValue = 0.65f,
      targetValue = if (isAnimating) 0f else 0f,
      animationSpec =
        infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Restart),
      label = "pulse_alpha",
    )

  Box(modifier = modifier.size(16.dp), contentAlignment = Alignment.Center) {
    if (isAnimating) {
      Box(
        modifier =
          Modifier.size(10.dp)
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
      )
    }
    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
  }
}
