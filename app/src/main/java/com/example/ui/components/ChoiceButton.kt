package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.StoryChoice
import com.example.ui.theme.*

@Composable
fun ChoiceButton(
    choice: StoryChoice,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFlowerChoice = choice.setsPegouFlor != null
    val borderColor = if (isFlowerChoice) {
        if (choice.setsPegouFlor == true) VioletLight else BorderGothic
    } else {
        BorderGothic
    }

    val containerColor = if (isFlowerChoice && choice.setsPegouFlor == true) {
        SurfaceCard.copy(alpha = 0.98f)
    } else {
        SurfaceGothic.copy(alpha = 0.95f)
    }

    OutlinedButton(
        onClick = onSelect,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .testTag("choice_button_${choice.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = PaleText
        ),
        border = BorderStroke(1.2.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = choice.text,
                color = if (isFlowerChoice && choice.setsPegouFlor == true) VioletLight else PaleText,
                fontSize = 14.sp,
                fontWeight = if (isFlowerChoice) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
