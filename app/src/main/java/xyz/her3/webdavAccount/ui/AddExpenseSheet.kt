package xyz.her3.webdavAccount.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 记一笔:详情 + 金额必填;日期默认今天、支出/收入默认支出(均可改)。
 * onSave(line, dateToken, onDone) 由调用方在子线程执行写入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onSave: (line: String, dateToken: String, onDone: (Result<Unit>) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isExpense by remember { mutableStateOf(true) }
    var detail by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val now = remember { Calendar.getInstance() }
    var timeH by remember { mutableIntStateOf(now.get(Calendar.HOUR_OF_DAY)) }
    var timeM by remember { mutableIntStateOf(now.get(Calendar.MINUTE)) }

    val displayFmt = remember { SimpleDateFormat("yyyy-MM-dd E", Locale.CHINA) }
    val tokenFmt = remember { SimpleDateFormat("yyMMdd", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = isExpense,
                    onClick = { isExpense = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("支出") },
                )
                SegmentedButton(
                    selected = !isExpense,
                    onClick = { isExpense = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("收入") },
                )
            }

            OutlinedTextField(
                value = detail,
                onValueChange = { detail = it },
                label = { Text("详情") },
                placeholder = { Text("比如:鸭腿饭") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("金额") },
                placeholder = { Text("0.00") },
                prefix = { Text("¥") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(displayFmt.format(Date(selectedMillis)))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("%02d:%02d".format(timeH, timeM))
                }
            }

            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val det = detail.trim()
                    val amt = amount.trim()
                    when {
                        det.isEmpty() -> saveError = "填一下详情"
                        !amt.matches(Regex("""\d+(\.\d{1,2})?""")) -> saveError = "金额格式不对"
                        else -> {
                            val normalized = BigDecimal(amt).stripTrailingZeros().toPlainString()
                            val token = tokenFmt.format(Date(selectedMillis))
                            val time = "%02d:%02d".format(timeH, timeM)
                            val line = "$token $time $det ${if (isExpense) "" else "+"}cny$normalized"
                            saving = true
                            saveError = null
                            onSave(line, token) { result ->
                                saving = false
                                result
                                    .onSuccess {
                                        onSaved()
                                        onDismiss()
                                    }
                                    .onFailure { saveError = "保存失败:${it.message}" }
                            }
                        }
                    }
                },
                enabled = !saving && detail.isNotBlank() && amount.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("保存")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = timeH,
            initialMinute = timeM,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    timeH = timePickerState.hour
                    timeM = timePickerState.minute
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
