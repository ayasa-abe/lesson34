package com.techacademy.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.techacademy.constants.ErrorKinds;
import com.techacademy.entity.Employee;
import com.techacademy.repository.EmployeeRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 従業員保存
    @Transactional
    public ErrorKinds save(Employee employee) {

        // パスワードチェック
        ErrorKinds result = employeePasswordCheck(employee);
        if (ErrorKinds.CHECK_OK != result) {
            return result;
        }

        // 従業員番号重複チェック
        if (findByCode(employee.getCode()) != null) {
            return ErrorKinds.DUPLICATE_ERROR;
        }

        employee.setDeleteFlg(false);

        LocalDateTime now = LocalDateTime.now();
        employee.setCreatedAt(now);
        employee.setUpdatedAt(now);

        employeeRepository.save(employee);
        return ErrorKinds.SUCCESS;
    }


    //　従業員更新
    @Transactional
    public ErrorKinds update(Employee employee, String code, String password, String role ) {

    	// 社員情報を取得(findByCodeメソッドを利用)
    	Employee motoemployee = findByCode(code);

    	// パスワードの入力欄が空の場合
    	if (password == null || password.isEmpty()) {
    	// 登録済のパスワードをそのまま使用
    	   password = motoemployee.getPassword();
    	} else {

    	    // パスワードの入力欄が空でない場合
    	    // パスワードチェックを行う(employeePasswordCheckメソッドを利用)
    	    // エラーの場合はemployeePasswordCheckメソッドの戻り値を返して終了
            ErrorKinds result = employeePasswordCheck(employee);
            if (ErrorKinds.CHECK_OK != result) {
                return result;
            }
        }
    	// employeeオブジェクトの必要な情報を設定していく
    	motoemployee.setName(employee.getName());  // 新しい名前に更新
    	if (password != null && !password.isEmpty()) {
    	    motoemployee.setPassword(password);  // 新しいパスワードに更新
    	    }
    	 // 権限の設定（プルダウンで選択された権限）
        if (role != null && !role.isEmpty()) {
            // String 型を Role 列挙型に変換
            Employee.Role newRole;
            if ("一般".equals(role)) {
                newRole = Employee.Role.GENERAL;
            } else if ("管理者".equals(role)) {
                newRole = Employee.Role.ADMIN;
            } else {
                // 無効な値が渡された場合はデフォルトの権限を設定
                newRole = Employee.Role.GENERAL;
            }
            motoemployee.setRole(newRole);  // "一般" または "管理者" の選択された権限を設定
        }
    	LocalDateTime now = LocalDateTime.now();  // 現在の日時を取得
        employee.setUpdatedAt(now);  // 従業員の更新日時を設定

        employeeRepository.save(employee);  // 従業員オブジェクトをデータベースに保存

    	// employeeRepository.(employee)で更新
    	employeeRepository.save(motoemployee);  // 更新された社員情報を保存

        return ErrorKinds.CHECK_OK;  // 更新成功
    	 }


    // 従業員削除
    @Transactional
    public ErrorKinds delete(String code, UserDetail userDetail) {

        // 自分を削除しようとした場合はエラーメッセージを表示
        if (code.equals(userDetail.getEmployee().getCode())) {
            return ErrorKinds.LOGINCHECK_ERROR;
        }
        Employee employee = findByCode(code);
        LocalDateTime now = LocalDateTime.now();
        employee.setUpdatedAt(now);
        employee.setDeleteFlg(true);

        return ErrorKinds.SUCCESS;
    }

    // 従業員一覧表示処理
    public List<Employee> findAll() {
        return employeeRepository.findAll();
    }

    // 1件を検索
    public Employee findByCode(String code) {
        // findByIdで検索
        Optional<Employee> option = employeeRepository.findById(code);
        // 取得できなかった場合はnullを返す
        Employee employee = option.orElse(null);
        return employee;
    }

    // 従業員パスワードチェック
    private ErrorKinds employeePasswordCheck(Employee employee) {

        // 従業員パスワードの半角英数字チェック処理
        if (isHalfSizeCheckError(employee)) {

            return ErrorKinds.HALFSIZE_ERROR;
        }

        // 従業員パスワードの8文字～16文字チェック処理
        if (isOutOfRangePassword(employee)) {

            return ErrorKinds.RANGECHECK_ERROR;
        }

        employee.setPassword(passwordEncoder.encode(employee.getPassword()));

        return ErrorKinds.CHECK_OK;
    }

    // 従業員パスワードの半角英数字チェック処理
    private boolean isHalfSizeCheckError(Employee employee) {

        // 半角英数字チェック
        Pattern pattern = Pattern.compile("^[A-Za-z0-9]+$");
        Matcher matcher = pattern.matcher(employee.getPassword());
        return !matcher.matches();
    }

    // 従業員パスワードの8文字～16文字チェック処理
    public boolean isOutOfRangePassword(Employee employee) {

        // 桁数チェック
        int passwordLength = employee.getPassword().length();
        return passwordLength < 8 || 16 < passwordLength;
    }

}
