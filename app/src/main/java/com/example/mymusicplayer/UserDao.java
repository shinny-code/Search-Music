package com.example.mymusicplayer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface UserDao {

    @Insert
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Query("SELECT * FROM users WHERE userId = :userId")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE email = :email AND password = :password")
    User getUserByEmailAndPassword(String email, String password);

    @Query("SELECT * FROM users WHERE email = :email")
    User getUserByEmail(String email);

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    User getLoggedInUser();

    @Query("SELECT * FROM users")
    List<User> getAllUsers();

    @Query("UPDATE users SET isLoggedIn = 0")
    void clearLoggedInUsers();

    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUser(int userId);
}