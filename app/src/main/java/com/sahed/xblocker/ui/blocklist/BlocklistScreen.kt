package com.sahed.xblocker.ui.blocklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahed.xblocker.domain.model.BlockedDomain
import com.sahed.xblocker.ui.theme.Accent
import com.sahed.xblocker.ui.theme.AccentDim
import com.sahed.xblocker.ui.theme.Amber
import com.sahed.xblocker.ui.theme.AmberDim
import com.sahed.xblocker.ui.theme.BgDark
import com.sahed.xblocker.ui.theme.Border
import com.sahed.xblocker.ui.theme.CardBg
import com.sahed.xblocker.ui.theme.Rose
import com.sahed.xblocker.ui.theme.TextMain
import com.sahed.xblocker.ui.theme.TextMuted
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BlocklistScreen(viewModel: BlocklistViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Blocklist",
            style = MaterialTheme.typography.headlineMedium,
            color = TextMain
        )

        // Add URL input
        AddDomainInput(
            value = state.inputText,
            error = state.inputError,
            isLoading = state.isAdding,
            onValueChange = viewModel::onInputChange,
            onAdd = viewModel::addDomain
        )

        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BlocklistFilter.values().forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.onFilterChange(filter) },
                    label = {
                        Text(
                            text = when (filter) {
                                BlocklistFilter.ALL -> "All"
                                BlocklistFilter.CUSTOM -> "Custom"
                                BlocklistFilter.PENDING -> "Pending"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentDim,
                        selectedLabelColor = Accent,
                        containerColor = CardBg,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = state.filter == filter,
                        borderColor = Border,
                        selectedBorderColor = Accent.copy(alpha = 0.4f)
                    )
                )
            }
        }

        // Domain list
        if (state.filteredDomains.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.filteredDomains, key = { it.id }) { domain ->
                    DomainCard(
                        domain = domain,
                        onDelete = { viewModel.requestDelete(domain) },
                        onCancelDelete = { timerId -> viewModel.cancelDelete(domain, timerId) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun AddDomainInput(
    value: String,
    error: String?,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. example.com or *.site.com", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Outlined.Language, contentDescription = null, tint = TextMuted)
            },
            trailingIcon = {
                IconButton(onClick = onAdd, enabled = !isLoading) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add domain", tint = Accent)
                }
            },
            isError = error != null,
            supportingText = error?.let { { Text(it, color = Rose) } },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Border,
                focusedTextColor = TextMain,
                unfocusedTextColor = TextMain,
                errorBorderColor = Rose
            )
        )
    }
}

@Composable
private fun DomainCard(
    domain: BlockedDomain,
    onDelete: () -> Unit,
    onCancelDelete: (Long) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (domain.pendingDelete) AmberDim else CardBg
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (domain.pendingDelete) Amber.copy(alpha = 0.3f) else Border
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (domain.pendingDelete) Amber.copy(0.15f) else AccentDim
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (domain.pendingDelete) Icons.Outlined.HourglassTop else Icons.Outlined.Language,
                    contentDescription = null,
                    tint = if (domain.pendingDelete) Amber else Accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Domain info
            Column(modifier = Modifier.weight(1f)) {
                Text(domain.domain, style = MaterialTheme.typography.titleSmall, color = TextMain)
                if (domain.pendingDelete && domain.deleteAt != null) {
                    val remainingMs = domain.deleteRemainingMs() ?: 0L
                    val h = remainingMs / 3_600_000
                    val m = (remainingMs % 3_600_000) / 60_000
                    val s = (remainingMs % 60_000) / 1_000
                    Text(
                        "Removes in %02d:%02d:%02d".format(h, m, s),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Amber
                    )
                } else {
                    Text(
                        "Added ${dateFormat.format(Date(domain.addedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            // Action button
            if (domain.pendingDelete) {
                TextButton(onClick = { onCancelDelete(0L) }) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(16.dp), tint = Amber)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel", color = Amber, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Rose)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.FilterList,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp)
            )
            Text("No domains here", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Text("Add a domain above to block it", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
