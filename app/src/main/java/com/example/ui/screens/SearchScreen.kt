package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.isDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.MountainEntity
import com.example.ui.util.PersianDateHelper
import com.example.ui.weather.WeatherViewModel
import com.example.ui.weather.WeatherUiState

enum class SortOrder(val label: String) {
    ALTITUDE_DESC("بیشترین ارتفاع"),
    ALTITUDE_ASC("کمترین ارتفاع"),
    NAME_ASC("ترتیب الفبا"),
    PINNED_FIRST("نشان‌شده‌ها ابتدا")
}

data class FamousPeakPreset(
    val name: String,
    val altitude: Int,
    val latitude: Double,
    val longitude: Double,
    val province: String,
    val range: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: WeatherViewModel,
    onMountainSelected: (MountainEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedProvince by viewModel.selectedProvince.collectAsStateWithLifecycle()
    val altitudeRange by viewModel.altitudeRange.collectAsStateWithLifecycle()
    val allMountains by viewModel.allMountains.collectAsStateWithLifecycle()
    val uiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()

    var currentSortOrder by remember { mutableStateOf(SortOrder.ALTITUDE_DESC) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingMountain by remember { mutableStateOf<MountainEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deletingMountain by remember { mutableStateOf<MountainEntity?>(null) }
    var customOnlyFilter by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val provinces = remember(allMountains, selectedType) {
        allMountains
            .filter { selectedType == null || it.type == selectedType }
            .map { it.persianProvince }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    // Dynamic elevation classifications depending on selected Category
    val elevationOptions = remember(selectedType) {
        when (selectedType) {
            "international_peak" -> listOf(
                ElevationFilterOption("همه موارد", 0 to 10000),
                ElevationFilterOption("ابر هشت‌هزارمتری‌ها (بالای ۸۰۰۰متر)", 8000 to 10000),
                ElevationFilterOption("بازه صعود آلپی (۶۰۰۰ تا ۸۰۰۰متر)", 6000 to 8000),
                ElevationFilterOption("بازه فنی قاره‌ای (۴۰۰۰ تا ۶۰۰۰متر)", 4000 to 6000),
                ElevationFilterOption("پکیج ارتفاعی پایه (زیر ۴۰۰۰متر)", 0 to 4000)
            )
            "ski_resort" -> listOf(
                ElevationFilterOption("همه موارد", 0 to 10000),
                ElevationFilterOption("پیست‌های ارتفاع بالا (بالای ۳۰۰۰متر)", 3000 to 10000),
                ElevationFilterOption("پیست‌های متوسط (۲۰۰۰ تا ۳۰۰۰متر)", 2000 to 3000),
                ElevationFilterOption("پیست‌های اقتصادی پایه (زیر ۲۰۰۰متر)", 0 to 2000)
            )
            else -> listOf(
                ElevationFilterOption("همه موارد", 0 to 10000),
                ElevationFilterOption("ابرقله‌ها (بالای ۴۰۰۰متر)", 4000 to 10000),
                ElevationFilterOption("مرتفع (۳۰۰۰ تا ۴۰۰۰متر)", 3000 to 4000),
                ElevationFilterOption("کوهپایه‌ای (زیر ۳۰۰۰متر)", 0 to 3000)
            )
        }
    }

    // Dynamic active filter counter
    val activeFiltersCount = remember(selectedProvince, altitudeRange, currentSortOrder, customOnlyFilter) {
        var count = 0
        if (selectedProvince != null) count++
        if (altitudeRange != (0 to 10000)) count++
        if (currentSortOrder != SortOrder.ALTITUDE_DESC) count++
        if (customOnlyFilter) count++
        count
    }

    // Apply Sorting and Filtering client-side dynamically
    val sortedResults = remember(searchResults, currentSortOrder, customOnlyFilter) {
        val filtered = if (customOnlyFilter) {
            searchResults.filter { it.isCustom }
        } else {
            searchResults
        }
        when (currentSortOrder) {
            SortOrder.ALTITUDE_DESC -> filtered.sortedByDescending { it.altitude }
            SortOrder.ALTITUDE_ASC -> filtered.sortedBy { it.altitude }
            SortOrder.NAME_ASC -> filtered.sortedBy { it.persianName }
            SortOrder.PINNED_FIRST -> filtered.sortedWith(
                compareByDescending<MountainEntity> { it.isPinned }
                    .thenByDescending { it.altitude }
            )
        }
    }

    // Dynamic counts for stats header
    val totalCount = allMountains.size
    val pinnedCount = allMountains.count { it.isPinned }
    val activeMountainId = (uiState as? WeatherUiState.Success)?.mountain?.id

    // Sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("search_screen_container")
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Sleek, Compact Header & Stats Row (Maximized space!)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "بانک جامع اطلاعات قله‌ها",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = "صعودهای فعال: ${PersianDateHelper.formatToPersianDigits(totalCount)} قله ثبت شده در سامانه",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                val isDark = MaterialTheme.colorScheme.background.isDark
                val premiumGoldColor = if (isDark) Color(0xFFFFD700) else Color(0xFF9A6A00)

                // Add peak trigger button
                FilledTonalButton(
                    onClick = { 
                        if (isPremium) {
                            showAddDialog = true 
                        } else {
                            viewModel.triggerBilling(true)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_custom_mountain_button")
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Default.Add else Icons.Default.Lock,
                        contentDescription = "ثبت قله جدید",
                        modifier = Modifier.size(14.dp),
                        tint = if (isPremium) MaterialTheme.colorScheme.primary else premiumGoldColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPremium) "قله جدید" else "قله جدید 🔒", 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black,
                        color = if (isPremium) MaterialTheme.colorScheme.onSurface else premiumGoldColor
                    )
                }
            }
        }

        // 2. High-UX Row: Search Bar & Floating BottomSheet Toggle Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_text_input"),
                placeholder = { 
                    Text(
                        text = "نام قله، استان، رشته‌کوه صعود...", 
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )

            // Dynamic bottom sheet filter button
            BadgedBox(
                badge = {
                    if (activeFiltersCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = PersianDateHelper.formatToPersianDigits(activeFiltersCount),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                IconButton(
                    onClick = { showFilterSheet = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (activeFiltersCount > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (activeFiltersCount > 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .size(48.dp) // meets accessibility target
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("filter_sheet_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "فیلتر و چیدمان پیشرفته",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 2b. Modern Category Selector (Iranian Peaks, International Peaks, Ski Resorts)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedType == "iran_peak",
                onClick = { viewModel.setSelectedType("iran_peak") },
                label = { Text("قله‌های ایران", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterHdr,
                        contentDescription = "Iran Peaks",
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedType == "international_peak",
                onClick = { viewModel.setSelectedType("international_peak") },
                label = { Text("برون‌مرزی", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "International Peaks",
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedType == "ski_resort",
                onClick = { viewModel.setSelectedType("ski_resort") },
                label = { Text("پیست‌های اسکی", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AcUnit,
                        contentDescription = "Ski Resorts",
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )

            FilterChip(
                selected = selectedType == null,
                onClick = { viewModel.setSelectedType(null) },
                label = { Text("همه موارد", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "All items",
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )
        }

        // 3. Compact Active Filter Chips list (Dismissable directly on main screen!)
        if (activeFiltersCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فیلترهای فعال:",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (currentSortOrder != SortOrder.ALTITUDE_DESC) {
                    InputChip(
                        selected = true,
                        onClick = { currentSortOrder = SortOrder.ALTITUDE_DESC },
                        label = { Text(currentSortOrder.label, fontSize = 9.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deactivate",
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                if (altitudeRange != (0 to 10000)) {
                    val labelText = when {
                        altitudeRange.first >= 8000 -> "بالای ۸۰۰۰متر"
                        altitudeRange.first >= 6000 -> "۶۰۰۰ تا ۸۰۰۰متر"
                        altitudeRange.first >= 4000 -> "۴۰۰۰ تا ۶۰۰۰متر"
                        altitudeRange.first >= 3000 -> "۳۰۰۰ تا ۴۰۰۰متر"
                        altitudeRange.second <= 4000 -> "زیر ۴۰۰۰متر"
                        else -> "ارتفاع خاص"
                    }
                    InputChip(
                        selected = true,
                        onClick = { viewModel.setAltitudeRange(0, 10000) },
                        label = { Text(labelText, fontSize = 9.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deactivate",
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                if (selectedProvince != null) {
                    InputChip(
                        selected = true,
                        onClick = { viewModel.setProvinceFilter(null) },
                        label = { Text(selectedProvince ?: "", fontSize = 9.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deactivate",
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                if (customOnlyFilter) {
                    InputChip(
                        selected = true,
                        onClick = { customOnlyFilter = false },
                        label = { Text("قله‌های سفارشی", fontSize = 9.sp) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Deactivate",
                                modifier = Modifier.size(11.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }

                // Quick clear text button
                Text(
                    text = "پاکسازی همه",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable {
                            currentSortOrder = SortOrder.ALTITUDE_DESC
                            viewModel.setAltitudeRange(0, 10000)
                            viewModel.setProvinceFilter(null)
                            customOnlyFilter = false
                        }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

        // 4. Inline Result Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نتایج یافت‌شده: ${PersianDateHelper.formatToPersianDigits(sortedResults.size)} قله صعود",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activeMountainId != null) {
                Text(
                    text = "با اولویت هدف فعال برنامه",
                    fontSize = 10.sp,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 5. Huge Clean Peak List (Now holds ~80% of screen real estate!)
        if (sortedResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterHdr,
                            contentDescription = "No results",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "هیچ قله‌ای با فیلترهای کنونی یافت نشد.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "می‌توانید فیلترها را پاکسازی کرده یا دکمه ثبت قله جدید را بزنید.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("search_results_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                items(sortedResults, key = { it.id }) { mount ->
                    MountainResultCard(
                        mountain = mount,
                        activeMountainId = activeMountainId,
                        onClick = { onMountainSelected(mount) },
                        onPinToggle = { viewModel.togglePin(mount) },
                        onEditClick = {
                            editingMountain = mount
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            deletingMountain = mount
                            showDeleteConfirmDialog = true
                        }
                    )
                }
            }
        }
    }

    // Modal Bottom Sheet Implementation (UX Breakthrough!)
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Sheet title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "تنظیمات و پالایش پیشرفته قله‌ها",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (activeFiltersCount > 0) {
                        TextButton(
                            onClick = {
                                currentSortOrder = SortOrder.ALTITUDE_DESC
                                viewModel.setAltitudeRange(0, 10000)
                                viewModel.setProvinceFilter(null)
                                customOnlyFilter = false
                            }
                        ) {
                            Text(
                                "پاکسازی فیلترها",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), thickness = 1.dp)

                // Segment 1: Sorting block
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "۱. اولویت‌بندی چیدمان قلل صعود",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SortOrder.values().forEach { order ->
                            val isSelected = currentSortOrder == order
                            FilterChip(
                                selected = isSelected,
                                onClick = { currentSortOrder = order },
                                label = { Text(order.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (order) {
                                            SortOrder.ALTITUDE_DESC -> Icons.AutoMirrored.Filled.TrendingDown
                                            SortOrder.ALTITUDE_ASC -> Icons.AutoMirrored.Filled.TrendingUp
                                            SortOrder.NAME_ASC -> Icons.Default.SortByAlpha
                                            SortOrder.PINNED_FIRST -> Icons.Default.Favorite
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    selectedBorderColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                }

                // Segment 2: Elevation block
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "۲. رده‌بندی ارتفاعی قله",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        elevationOptions.forEach { option ->
                            val isSelected = altitudeRange == option.range
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setAltitudeRange(option.range.first, option.range.second)
                                },
                                label = { Text(option.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Segment 3: Province block
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "۳. پالایش سریع بر پایه استان جغرافیایی",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isAllSelected = selectedProvince == null
                        FilterChip(
                            selected = isAllSelected,
                            onClick = { viewModel.setProvinceFilter(null) },
                            label = { Text("همه استان‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isAllSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        provinces.forEach { prov ->
                            val isSelected = selectedProvince == prov
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setProvinceFilter(if (isSelected) null else prov)
                                },
                                label = { Text(prov, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    selectedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Segment 4: Custom Peaks filter
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "۴. منبع قله‌های صعود",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !customOnlyFilter,
                            onClick = { customOnlyFilter = false },
                            label = { Text("همه قله‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !customOnlyFilter,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        FilterChip(
                            selected = customOnlyFilter,
                            onClick = { customOnlyFilter = true },
                            label = { Text("فقط قله‌های شخصی کاربر", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = customOnlyFilter,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sheet Apply/Confirm Button
                Button(
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_filters_sheet_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "مشاهده ${PersianDateHelper.formatToPersianDigits(sortedResults.size)} قله صعود",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add Mountain Dialog (Pre-configured Dialog blocks)
    if (showAddDialog) {
        var inputName by remember { mutableStateOf("") }
        var inputAltitude by remember { mutableStateOf("") }
        var inputLatitude by remember { mutableStateOf("") }
        var inputLongitude by remember { mutableStateOf("") }
        var inputProvince by remember { mutableStateOf("") }
        var inputRange by remember { mutableStateOf("") }
        var inputType by remember { mutableStateOf("iran_peak") }
        
        var nameError by remember { mutableStateOf(false) }
        var altitudeError by remember { mutableStateOf(false) }
        var latitudeError by remember { mutableStateOf(false) }
        var longitudeError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "ثبت فنی قله جدید (مختصات GPS)",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "مختصات فنی و ارتفاع نقطه هدف را وارد کنید تا سامانه‌های پایش اقلیم و دیسپچ نجومی برنامه صعود فعال شوند.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    // Famous Presets Quick Chips
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "الگوهای آماده قله‌های شاخص (تک‌لمس):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    FamousPeakPreset("علم‌کوه", 4850, 36.3761, 50.9636, "مازندران", "البرز مرکزی"),
                                    FamousPeakPreset("سبلان", 4811, 38.2672, 47.8394, "اردبیل", "سهند و سبلان"),
                                    FamousPeakPreset("دماوند", 5671, 35.9550, 52.1106, "مازندران", "البرز مرکزی"),
                                    FamousPeakPreset("آزادکوه", 4355, 36.1706, 51.5039, "مازندران", "البرز مرکزی"),
                                    FamousPeakPreset("کلون‌بستک", 4180, 36.0500, 51.4833, "تهران", "البرز مرکزی"),
                                    FamousPeakPreset("اورست", 8848, 27.9881, 86.9250, "نپال/چین", "هیمالیا"),
                                    FamousPeakPreset("K2", 8611, 35.8808, 76.5158, "پاکستان/چین", "قراقروم")
                                ).forEach { preset ->
                                    SuggestionChip(
                                        onClick = {
                                            inputName = preset.name
                                            inputAltitude = preset.altitude.toString()
                                            inputLatitude = preset.latitude.toString()
                                            inputLongitude = preset.longitude.toString()
                                            inputProvince = preset.province
                                            inputRange = preset.range
                                            inputType = if (preset.altitude > 6000) "international_peak" else "iran_peak"
                                            nameError = false
                                            altitudeError = false
                                            latitudeError = false
                                            longitudeError = false
                                        },
                                        label = { Text(preset.name, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        icon = { Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    )
                                }
                            }
                        }
                    }

                    // Type Segment Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "نوع پایگاه صعود (دسته‌بندی اصلی)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("iran_peak", "قله ایران", Icons.Default.FilterHdr),
                                    Triple("international_peak", "برون‌مرزی", Icons.Default.Language),
                                    Triple("ski_resort", "پیست اسکی", Icons.Default.AcUnit)
                                ).forEach { (typeKey, typeLabel, typeIcon) ->
                                    val isSelected = inputType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { inputType = typeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = typeIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = typeLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Form Inputs
                    item {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = {
                                inputName = it
                                nameError = it.isBlank()
                            },
                            label = { Text("نام قله یا ایستگاه فرعی صعود", fontSize = 11.sp) },
                            placeholder = { Text("مثال: قله شاه البرز طالقان", fontSize = 11.sp) },
                            isError = nameError,
                            supportingText = if (nameError) {
                                { Text("وارد کردن نام الزامی است", color = MaterialTheme.colorScheme.error, fontSize = 9.sp) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_input_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputAltitude,
                            onValueChange = {
                                inputAltitude = it
                                val normalized = PersianDateHelper.normalizePersianDigits(it)
                                val v = normalized.toIntOrNull()
                                altitudeError = v == null || v !in 1..9000
                            },
                            label = { Text("ارتفاع مرز صعود (متر)", fontSize = 11.sp) },
                            placeholder = { Text("عددی بین ۱ تا ۹۰۰۰ (مثلا ۴۱۳۵)", fontSize = 11.sp) },
                            isError = altitudeError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = if (altitudeError) {
                                { Text("ارتفاع معتبر نیست (۱ الی ۹۰۰۰ متر)", color = MaterialTheme.colorScheme.error, fontSize = 9.sp) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("dialog_input_altitude"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputLatitude,
                                onValueChange = {
                                    inputLatitude = it
                                    val normalized = PersianDateHelper.normalizePersianDigits(it)
                                    val v = normalized.toDoubleOrNull()
                                    latitudeError = v == null || v !in -90.0..90.0
                                },
                                label = { Text("عرض جغرافیایی (Lat)", fontSize = 10.sp) },
                                placeholder = { Text("مثال ۳۶.۲۱", fontSize = 10.sp) },
                                isError = latitudeError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = if (latitudeError) {
                                    { Text("بین -۹۰ تا ۹۰", color = MaterialTheme.colorScheme.error, fontSize = 8.sp) }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_input_latitude"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            OutlinedTextField(
                                value = inputLongitude,
                                onValueChange = {
                                    inputLongitude = it
                                    val normalized = PersianDateHelper.normalizePersianDigits(it)
                                    val v = normalized.toDoubleOrNull()
                                    longitudeError = v == null || v !in -180.0..180.0
                                },
                                label = { Text("طول جغرافیایی (Lng)", fontSize = 10.sp) },
                                placeholder = { Text("مثال ۵۱.۲۵", fontSize = 10.sp) },
                                isError = longitudeError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = if (longitudeError) {
                                    { Text("بین -۱۸۰ تا ۱۸۰", color = MaterialTheme.colorScheme.error, fontSize = 8.sp) }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_input_longitude"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputProvince,
                                onValueChange = { inputProvince = it },
                                label = { Text("استان", fontSize = 11.sp) },
                                placeholder = { Text("مثال: مازندران", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_input_province"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            OutlinedTextField(
                                value = inputRange,
                                onValueChange = { inputRange = it },
                                label = { Text("رشته کوه / یال صعود", fontSize = 11.sp) },
                                placeholder = { Text("مثال: البرز غربی", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("dialog_input_range"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val altVal = PersianDateHelper.normalizePersianDigits(inputAltitude).toIntOrNull()
                val latVal = PersianDateHelper.normalizePersianDigits(inputLatitude).toDoubleOrNull()
                val lngVal = PersianDateHelper.normalizePersianDigits(inputLongitude).toDoubleOrNull()
                
                val isFormValid = inputName.isNotBlank() &&
                        altVal != null && altVal in 1..9000 &&
                        latVal != null && latVal >= -90.0 && latVal <= 90.0 &&
                        lngVal != null && lngVal >= -180.0 && lngVal <= 180.0
                
                Button(
                    enabled = isFormValid,
                    onClick = {
                        val alt = altVal ?: 0
                        val lat = latVal ?: 0.0
                        val lng = lngVal ?: 0.0
                        val prov = if (inputProvince.isNotBlank()) inputProvince else "سفارشی"
                        val rng = if (inputRange.isNotBlank()) inputRange else "سفارشی"
                        
                        viewModel.addNewMountain(
                            persianName = inputName,
                            altitude = alt,
                            latitude = lat,
                            longitude = lng,
                            province = prov,
                            range = rng,
                            type = inputType
                        )
                        showAddDialog = false
                        
                        onMountainSelected(
                            MountainEntity(
                                id = 0,
                                name = inputName,
                                persianName = inputName,
                                province = prov,
                                persianProvince = prov,
                                range = rng,
                                latitude = lat,
                                longitude = lng,
                                altitude = alt,
                                isPinned = true,
                                isCustom = true,
                                type = inputType
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.testTag("dialog_confirm_button")
                ) {
                    Text("ثبت پایگاه صعود و پایش هواشناسی", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("انصراف", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    if (showEditDialog && editingMountain != null) {
        val mountain = editingMountain!!
        var inputName by remember(mountain) { mutableStateOf(mountain.persianName) }
        var inputAltitude by remember(mountain) { mutableStateOf(mountain.altitude.toString()) }
        var inputLatitude by remember(mountain) { mutableStateOf(mountain.latitude.toString()) }
        var inputLongitude by remember(mountain) { mutableStateOf(mountain.longitude.toString()) }
        var inputProvince by remember(mountain) { mutableStateOf(mountain.persianProvince) }
        var inputRange by remember(mountain) { mutableStateOf(mountain.range) }
        var inputType by remember(mountain) { mutableStateOf(mountain.type ?: "iran_peak") }
        
        var nameError by remember { mutableStateOf(false) }
        var altitudeError by remember { mutableStateOf(false) }
        var latitudeError by remember { mutableStateOf(false) }
        var longitudeError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "ویرایش فنی قله سفارشی",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type Segment Selector
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "نوع پایگاه صعود (دسته‌بندی اصلی)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    Triple("iran_peak", "قله ایران", Icons.Default.FilterHdr),
                                    Triple("international_peak", "برون‌مرزی", Icons.Default.Language),
                                    Triple("ski_resort", "پیست اسکی", Icons.Default.AcUnit)
                                ).forEach { (typeKey, typeLabel, typeIcon) ->
                                    val isSelected = inputType == typeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { inputType = typeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = typeIcon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = typeLabel,
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = {
                                inputName = it
                                nameError = it.isBlank()
                            },
                            label = { Text("نام قله یا ایستگاه فرعی صعود", fontSize = 11.sp) },
                            isError = nameError,
                            supportingText = if (nameError) {
                                { Text("وارد کردن نام الزامی است", color = MaterialTheme.colorScheme.error, fontSize = 9.sp) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = inputAltitude,
                            onValueChange = {
                                inputAltitude = it
                                val normalized = PersianDateHelper.normalizePersianDigits(it)
                                val v = normalized.toIntOrNull()
                                altitudeError = v == null || v !in 1..9000
                            },
                            label = { Text("ارتفاع مرز صعود (متر)", fontSize = 11.sp) },
                            isError = altitudeError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = if (altitudeError) {
                                { Text("ارتفاع معتبر نیست (۱ الی ۹۰۰۰ متر)", color = MaterialTheme.colorScheme.error, fontSize = 9.sp) }
                            } else null,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputLatitude,
                                onValueChange = {
                                    inputLatitude = it
                                    val normalized = PersianDateHelper.normalizePersianDigits(it)
                                    val v = normalized.toDoubleOrNull()
                                    latitudeError = v == null || v !in -90.0..90.0
                                },
                                label = { Text("عرض جغرافیایی (Lat)", fontSize = 10.sp) },
                                isError = latitudeError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = if (latitudeError) {
                                    { Text("بین -۹۰ تا ۹۰", color = MaterialTheme.colorScheme.error, fontSize = 8.sp) }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            OutlinedTextField(
                                value = inputLongitude,
                                onValueChange = {
                                    inputLongitude = it
                                    val normalized = PersianDateHelper.normalizePersianDigits(it)
                                    val v = normalized.toDoubleOrNull()
                                    longitudeError = v == null || v !in -180.0..180.0
                                },
                                label = { Text("طول جغرافیایی (Lng)", fontSize = 10.sp) },
                                isError = longitudeError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                supportingText = if (longitudeError) {
                                    { Text("بین -۱۸۰ تا ۱۸۰", color = MaterialTheme.colorScheme.error, fontSize = 8.sp) }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = inputProvince,
                                onValueChange = { inputProvince = it },
                                label = { Text("استان", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )

                            OutlinedTextField(
                                value = inputRange,
                                onValueChange = { inputRange = it },
                                label = { Text("رشته کوه / یال صعود", fontSize = 11.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                val altVal = PersianDateHelper.normalizePersianDigits(inputAltitude).toIntOrNull()
                val latVal = PersianDateHelper.normalizePersianDigits(inputLatitude).toDoubleOrNull()
                val lngVal = PersianDateHelper.normalizePersianDigits(inputLongitude).toDoubleOrNull()
                
                val isFormValid = inputName.isNotBlank() &&
                        altVal != null && altVal in 1..9000 &&
                        latVal != null && latVal >= -90.0 && latVal <= 90.0 &&
                        lngVal != null && lngVal >= -180.0 && lngVal <= 180.0
                
                Button(
                    enabled = isFormValid,
                    onClick = {
                        val alt = altVal ?: 0
                        val lat = latVal ?: 0.0
                        val lng = lngVal ?: 0.0
                        val prov = if (inputProvince.isNotBlank()) inputProvince else "سفارشی"
                        val rng = if (inputRange.isNotBlank()) inputRange else "سفارشی"
                        
                        val updated = mountain.copy(
                            persianName = inputName,
                            name = inputName,
                            altitude = alt,
                            latitude = lat,
                            longitude = lng,
                            province = prov,
                            persianProvince = prov,
                            range = rng,
                            type = inputType
                        )
                        viewModel.updateMountain(updated)
                        showEditDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("dialog_edit_confirm_button")
                ) {
                    Text("ذخیره تغییرات", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("انصراف", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }

    if (showDeleteConfirmDialog && deletingMountain != null) {
        val mountain = deletingMountain!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "حذف قله سفارشی",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "آیا واقعاً می‌خواهید قله سفارشی «${mountain.persianName}» را حذف کنید؟ این عمل غیرقابل بازگشت است و صعود ثبت‌شده برای این نقطه لغو و اطلاعات حذف خواهد شد.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMountain(mountain)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف قله", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("انصراف", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        )
    }
}

@Composable
fun MountainResultCard(
    mountain: MountainEntity,
    activeMountainId: Int?,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val isActive = activeMountainId == mountain.id

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_mountain_card_${mountain.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                Color(0xFF4CAF50).copy(alpha = 0.04f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row #1: Title Group (L) and Elevation Badge (R)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val icon = when {
                        mountain.isCustom -> Icons.Default.Person
                        mountain.type == "ski_resort" -> Icons.Default.AcUnit
                        mountain.type == "international_peak" -> Icons.Default.Language
                        else -> Icons.Default.FilterHdr
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) Color(0xFF4CAF50).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Peak symbol",
                            tint = if (isActive) Color(0xFF388E3C) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = mountain.persianName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val categoryText = when {
                                mountain.isCustom -> "سفارشی"
                                mountain.type == "ski_resort" -> "پیست اسکی"
                                mountain.type == "international_peak" -> "برون‌مرزی"
                                else -> "قله ایران"
                            }
                            val categoryColor = when {
                                mountain.isCustom -> MaterialTheme.colorScheme.secondary
                                mountain.type == "ski_resort" -> Color(0xFF03A9F4)
                                mountain.type == "international_peak" -> Color(0xFF9C27B0)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(categoryColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = categoryText,
                                    fontSize = 9.sp,
                                    color = categoryColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Province",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${mountain.persianProvince} • رشته‌کوه ${mountain.range}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Elevation Badge Only on the Right Side of Row #1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${PersianDateHelper.formatToPersianDigits(mountain.altitude)} م",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Row #2: Professional Geolocation Metadata Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CompassCalibration,
                        contentDescription = "Coordinates",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "عرض: ${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.4f", mountain.latitude))} | طول: ${PersianDateHelper.formatToPersianDigits(String.format(java.util.Locale.US, "%.4f", mountain.longitude))}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // Row #3: Professional Divider & Bottom Action Toolbar to isolate functions cleanly
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                thickness = 1.dp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Toggle Button (Always visible on all mountains)
                TextButton(
                    onClick = onPinToggle,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (mountain.isPinned) Color(0xFFFF1744) else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("pin_toggle_${mountain.id}").height(32.dp)
                ) {
                    Icon(
                        imageVector = if (mountain.isPinned) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle bookmark",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (mountain.isPinned) "نشان‌شده" else "افزودن به نشان‌ها",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Edit/Delete Action Buttons (Visible only on User Custom Mountains)
                if (mountain.isCustom) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit Button
                        FilledTonalButton(
                            onClick = { onEditClick?.invoke() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Peak Details",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ویرایش", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Delete Button
                        FilledTonalButton(
                            onClick = { onDeleteClick?.invoke() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Peak",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

data class ElevationFilterOption(
    val title: String,
    val range: Pair<Int, Int>
)
