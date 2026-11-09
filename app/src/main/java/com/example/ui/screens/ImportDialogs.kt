package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FormulaType
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TxtImportDialog(
    onDismiss: () -> Unit,
    onImportTxt: (String) -> Unit,
    onLoadSample: () -> Unit
) {
    val context = LocalContext.current
    var textContent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Extra1X2 TXT Kolonları Yükle",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Her satırda 15 karakter (1, X, 2 veya 0) olacak şekilde kolonlarınızı yapıştırınız:",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = textContent,
                    onValueChange = { textContent = it },
                    placeholder = {
                        Text(
                            "Örn:\n1X211X212111X21\n11111X212111X21\n1X2111112111X21",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("input_txt_content"),
                    maxLines = 8,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        onLoadSample()
                        onDismiss()
                        Toast.makeText(context, "Örnek 9 Kolonluk Extra1X2 TXT yüklendi!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_load_sample_txt"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text("Örnek Extra1X2 Kolonlarını Doldur", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textContent.isNotBlank()) {
                        onImportTxt(textContent)
                        onDismiss()
                        Toast.makeText(context, "TXT Kolonları başarıyla kupona aktarıldı!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lütfen en az bir kolon metni giriniz.", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_import_txt")
            ) {
                Text("Kupona Yükle", color = Color.White, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextMuted)
            }
        }
    )
}

@Composable
fun ExcelImportDialog(
    onDismiss: () -> Unit,
    onImportContent: (String) -> Unit
) {
    val context = LocalContext.current
    var excelData by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Excel Kuponlarını Yükle",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = "Excel veya CSV formatındaki 15 maçlık kolon verilerini (sekme veya virgülle ayrılmış) yapıştırınız:",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = excelData,
                    onValueChange = { excelData = it },
                    placeholder = {
                        Text(
                            "Kolon1\tKolon2\n1\t1\nX\t1\n2\tX ...",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = ElectricCyan,
                        unfocusedIndicatorColor = DarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("input_excel_content"),
                    maxLines = 8,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        excelData = "1X211X212111X21\n111111111111111\n222222222222222\nXXXXXXXXXXXXXXX"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Text("Örnek Excel Verisi Ekle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (excelData.isNotBlank()) {
                        onImportContent(excelData)
                        onDismiss()
                        Toast.makeText(context, "Excel kolonları başarıyla kupona aktarıldı!", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_import_excel")
            ) {
                Text("Excel'den Yükle", color = Color.White, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal", color = TextMuted)
            }
        }
    )
}
