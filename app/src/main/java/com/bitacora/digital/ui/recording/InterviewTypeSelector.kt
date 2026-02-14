package com.bitacora.digital.ui.recording

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.bitacora.digital.model.InterviewType
import com.bitacora.digital.model.InterviewTypeInfo
import com.bitacora.digital.model.PracticeSubcategory
import com.bitacora.digital.model.getInterviewTypeInfo
import com.bitacora.digital.model.interviewTypes

/**
 * Full interview type selector for pre-recording setup.
 */
@Composable
fun InterviewTypeSelector(
    selectedType: InterviewType,
    subcategory: PracticeSubcategory?,
    userName: String,
    onTypeSelected: (InterviewType, PracticeSubcategory?) -> Unit,
    onUserNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Name input
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Your Name (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            BasicTextField(
                value = userName,
                onValueChange = onUserNameChanged,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        if (userName.isEmpty()) {
                            Text(
                                text = "Enter your name",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        // Type selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Session Type",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(280.dp)
            ) {
                items(interviewTypes) { info ->
                    InterviewTypeCard(
                        info = info,
                        isSelected = info.type == selectedType && info.subcategory == subcategory,
                        onClick = { onTypeSelected(info.type, info.subcategory) }
                    )
                }
            }
        }
    }
}

/**
 * Individual type card.
 */
@Composable
private fun InterviewTypeCard(
    info: InterviewTypeInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Icon(
            imageVector = info.icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(28.dp)
        )

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.label,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 2
            )
        }

        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Compact type selector shown during recording.
 */
@Composable
fun CompactTypeSelector(
    selectedType: InterviewType,
    subcategory: PracticeSubcategory?,
    disabled: Boolean,
    onTypeSelected: (InterviewType, PracticeSubcategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val currentInfo = getInterviewTypeInfo(selectedType, subcategory)

    Column(modifier = modifier) {
        // Current selection button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(enabled = !disabled) { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            currentInfo?.let { info ->
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = info.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color.White,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (isExpanded) 180f else 0f)
            )
        }

        // Dropdown list
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
            ) {
                interviewTypes.forEach { info ->
                    val isItemSelected = info.type == selectedType && info.subcategory == subcategory

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onTypeSelected(info.type, info.subcategory)
                                isExpanded = false
                            }
                            .background(
                                if (isItemSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = info.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = info.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
