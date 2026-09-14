package com.example.controledegastos.data.local.dao

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.controledegastos.data.model.Items

@Dao
interface ItemsDao{

    @Insert
    suspend fun insertItem(items: Items)

    @Insert
    suspend fun insertAllItems(items: List<Items>)

    @Update
    suspend fun updateItem(items: Items)

    @Query("Delete from Items where id = :id")
    suspend fun deleteItem(id:Int)

    @Query("DELETE FROM Items WHERE yearMonth = :yearMonth")
    suspend fun deleteItemMonth(yearMonth: Int)

    @Query("SELECT * FROM Items ORDER BY occurredAtMillis DESC, id DESC")
    fun getAllItems(): Flow<List<Items>>

    @Query("SELECT * FROM Items WHERE yearMonth = :yearMonth ORDER BY occurredAtMillis DESC, id DESC")
    fun getMonth(yearMonth: Int): Flow<List<Items>>

    //Filtro por entrada/saída de determinado mês
    @Query("SELECT * FROM Items WHERE yearMonth = :yearMonth AND io = :flow ORDER BY occurredAtMillis DESC, id DESC")
    fun getMonthFlow(yearMonth: Int, flow: String): Flow<List<Items>>

    @Query("SELECT * FROM Items WHERE io = :io ORDER BY occurredAtMillis DESC, id DESC")
    fun getIOFiltered(io: String): Flow<List<Items>>

    @Query("SELECT * FROM Items WHERE category = :category ORDER BY occurredAtMillis DESC, id DESC")
    fun getCategory(category: String): Flow<List<Items>>

    //Filtro por mês e categoria
    @Query("SELECT * FROM Items WHERE yearMonth = :yearMonth AND category = :category ORDER BY occurredAtMillis DESC, id DESC")
    fun getMonthCtg(yearMonth: Int, category: String): Flow<List<Items>>

}
