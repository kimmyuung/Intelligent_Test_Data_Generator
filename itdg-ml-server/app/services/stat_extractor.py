import pandas as pd
import numpy as np

def extract_statistics(df: pd.DataFrame) -> dict:
    stats = {}
    
    for column in df.columns:
        col_data = df[column]
        col_stats = {
            "name": column,
            "type": str(col_data.dtype),
            "count": int(col_data.count()),
            "null_count": int(col_data.isnull().sum())
        }
        
        # Numeric Stats
        if pd.api.types.is_numeric_dtype(col_data):
            col_stats["category"] = "numeric"
            col_stats["min"] = float(col_data.min()) if not pd.isna(col_data.min()) else None
            col_stats["max"] = float(col_data.max()) if not pd.isna(col_data.max()) else None
            col_stats["mean"] = float(col_data.mean()) if not pd.isna(col_data.mean()) else None
            # col_stats["std"] = float(col_data.std()) if not pd.isna(col_data.std()) else None
            
        # Categorical / String Stats
        else:
            col_stats["category"] = "categorical"
            # Get top 5 frequent values
            value_counts = col_data.value_counts(dropna=True).head(5).to_dict()
            col_stats["top_values"] = {str(k): int(v) for k, v in value_counts.items()}
            col_stats["unique_count"] = int(col_data.nunique())
            
        stats[column] = col_stats
        
    return stats
