package pe.edu.upeu.excel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Service
public class ReadAndWriteService {

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ReadAndWriteService(DataSource dataSource, PasswordEncoder passwordEncoder) {
        this.dataSource = dataSource;
        this.passwordEncoder = passwordEncoder;
    }

    public void leerYSubirExcel(String rutaArchivoExcel) {
        try (FileInputStream fis = new FileInputStream(new File(rutaArchivoExcel));
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String nombreTabla = sheet.getRow(0).getCell(0).getStringCellValue();

                Row filaColumnas = sheet.getRow(1);
                int numColumnas = filaColumnas.getLastCellNum();
                StringBuilder columnas = new StringBuilder();

                for (int j = 1; j < numColumnas; j++) {
                    columnas.append(filaColumnas.getCell(j).getStringCellValue());
                    if (j < numColumnas - 1) {
                        columnas.append(", ");
                    }
                }

                StringBuilder query = new StringBuilder("INSERT INTO " + nombreTabla + " (id" + nombreTabla + ", " + columnas + ") VALUES (");
                query.append("sq_" + nombreTabla + ".NEXTVAL");

                for (int j = 1; j < numColumnas; j++) {
                    query.append(", ?");
                }
                query.append(")");

                int registrosInsertados = 0;
                for (int j = 2; j <= sheet.getLastRowNum(); j++) {
                    Row row = sheet.getRow(j);
                    if (row == null || isRowEmpty(row)) {
                        continue;
                    }
                    Object[] valores = new Object[numColumnas - 1];
                    for (int k = 1; k < numColumnas; k++) {
                        Cell cell = row.getCell(k);
                        if (cell != null) {
                            // Encriptar contraseña solo si es la tabla 'usuario' y la columna es 'password'
                            if ("usuario".equalsIgnoreCase(nombreTabla) &&
                                "password".equalsIgnoreCase(filaColumnas.getCell(k).getStringCellValue())) {
                                valores[k - 1] = passwordEncoder.encode(cell.getStringCellValue());
                            } else {
                                switch (cell.getCellType()) {
                                    case STRING:
                                        valores[k - 1] = cell.getStringCellValue();
                                        break;
                                    case NUMERIC:
                                        valores[k - 1] = cell.getNumericCellValue();
                                        break;
                                    case BOOLEAN:
                                        valores[k - 1] = cell.getBooleanCellValue();
                                        break;
                                    default:
                                        valores[k - 1] = null;
                                }
                            }
                        }
                    }

                    insertarEnBaseDeDatos(query.toString(), valores);
                    registrosInsertados++;
                }

                System.out.println("Datos insertados correctamente en la tabla " + nombreTabla + ": " + registrosInsertados + " registros.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertarEnBaseDeDatos(String query, Object[] valores) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement pstmt = conexion.prepareStatement(query)) {
            for (int i = 0; i < valores.length; i++) {
                pstmt.setObject(i + 1, valores[i]);
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}