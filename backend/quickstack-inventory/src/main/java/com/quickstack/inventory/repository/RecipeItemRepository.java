package com.quickstack.inventory.repository;

import com.quickstack.inventory.entity.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for RecipeItem entity.
 * Supports bulk delete by recipeId for upsert semantics in RecipeService.
 */
@Repository
public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {

    @Query("SELECT ri FROM RecipeItem ri WHERE ri.recipeId = :recipeId ORDER BY ri.createdAt ASC")
    List<RecipeItem> findAllByRecipeId(@Param("recipeId") UUID recipeId);

    @Modifying
    @Query("DELETE FROM RecipeItem ri WHERE ri.recipeId = :recipeId")
    void deleteAllByRecipeId(@Param("recipeId") UUID recipeId);

    @Query("SELECT CASE WHEN COUNT(ri) > 0 THEN true ELSE false END FROM RecipeItem ri WHERE ri.recipeId = :recipeId AND ri.ingredientId = :ingredientId")
    boolean existsByRecipeIdAndIngredientId(
            @Param("recipeId") UUID recipeId,
            @Param("ingredientId") UUID ingredientId);
}
