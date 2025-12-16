import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

/**
 * Helper to download data in various formats
 */
const ExportUtils = {
    // 1. JSON Export
    downloadJson: (data, filename = 'generated_data.json') => {
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        saveAs(blob, filename);
    },

    // 2. CSV Export (Simples: If multiple tables, maybe zip? For now, we'll just concat or support single table logic)
    // To keep it simple and robust: We will generate one CSV per table. 
    // If there were multiple tables in 'data', we might need a ZIP. 
    // Here, we'll assume 'data' is the map: { tableName: [rows...] }
    // We'll create a simple text format:
    // --- Table: ABC ---
    // col1,col2
    // val1,val2
    downloadCsv: (data, filename = 'generated_data.csv') => {
        let csvContent = "";

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!rows || rows.length === 0) return;

            csvContent += `TABLE: ${tableName}\n`;

            // Headers
            const headers = Object.keys(rows[0]);
            csvContent += headers.join(",") + "\n";

            // Rows
            rows.forEach(row => {
                const values = headers.map(header => {
                    const val = row[header];
                    // Escape quotes and wrap in quotes if contains comma
                    const stringVal = String(val === null || val === undefined ? '' : val);
                    if (stringVal.includes(",") || stringVal.includes('"') || stringVal.includes("\n")) {
                        return `"${stringVal.replace(/"/g, '""')}"`;
                    }
                    return stringVal;
                });
                csvContent += values.join(",") + "\n";
            });
            csvContent += "\n";
        });

        const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        saveAs(blob, filename);
    },

    // 3. SQL Export
    downloadSql: (data, filename = 'generated_data.sql') => {
        let sqlContent = "";

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!rows || rows.length === 0) return;

            sqlContent += `-- Data for table: ${tableName}\n`;

            rows.forEach(row => {
                const headers = Object.keys(row);
                const values = headers.map(key => {
                    const val = row[key];
                    if (val === null || val === undefined) return 'NULL';
                    if (typeof val === 'number') return val;
                    if (typeof val === 'boolean') return val ? 1 : 0; // or true/false depending on DB
                    // Escape single quotes
                    return `'${String(val).replace(/'/g, "''")}'`;
                });

                sqlContent += `INSERT INTO ${tableName} (${headers.join(", ")}) VALUES (${values.join(", ")});\n`;
            });
            sqlContent += "\n";
        });

        const blob = new Blob([sqlContent], { type: 'text/plain;charset=utf-8' });
        saveAs(blob, filename);
    },

    // 4. Excel (XLSX / XLS) Export
    downloadExcel: (data, filename = 'generated_data', format = 'xlsx') => {
        const wb = XLSX.utils.book_new();

        Object.keys(data).forEach(tableName => {
            const rows = data[tableName];
            if (!rows || rows.length === 0) return;

            const ws = XLSX.utils.json_to_sheet(rows);
            // Sheet name max length 31 chars in Excel
            const sheetName = tableName.length > 30 ? tableName.substring(0, 30) : tableName;
            XLSX.utils.book_append_sheet(wb, ws, sheetName);
        });

        // Write file
        XLSX.writeFile(wb, `${filename}.${format}`);
    }
};

export default ExportUtils;
