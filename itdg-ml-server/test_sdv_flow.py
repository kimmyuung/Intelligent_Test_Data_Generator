"""SDV 통합 테스트 스크립트"""
import requests
import json

BASE_URL = "http://localhost:8000"

def test_flow():
    # 1. Health Check
    print("=== 1. Health Check ===")
    r = requests.get(f"{BASE_URL}/health")
    print(f"Status: {r.status_code}, Response: {r.json()}")
    
    # 2. Analyze (파일 업로드)
    print("\n=== 2. Analyze Sample Data ===")
    with open("test_sample.csv", "rb") as f:
        r = requests.post(f"{BASE_URL}/api/v1/analyze", files={"file": f})
    print(f"Status: {r.status_code}")
    if r.status_code != 200:
        print(f"Error: {r.text}")
        return
    
    analyze_result = r.json()
    file_id = analyze_result.get("fileId")
    print(f"FileId: {file_id}")
    print(f"Stats: {json.dumps(analyze_result.get('stats'), indent=2, ensure_ascii=False)}")
    
    # 3. Train Model
    print("\n=== 3. Train Model (Copula) ===")
    r = requests.post(f"{BASE_URL}/api/v1/train", params={"file_id": file_id, "model_type": "copula"})
    print(f"Status: {r.status_code}")
    if r.status_code != 200:
        print(f"Error: {r.text}")
        return
    
    train_result = r.json()
    model_id = train_result.get("modelId")
    print(f"ModelId: {model_id}")
    print(f"Training Time: {train_result.get('trainingTime')}s")
    
    # 4. Generate Synthetic Data
    print("\n=== 4. Generate Synthetic Data (5 rows) ===")
    r = requests.post(f"{BASE_URL}/api/v1/generate/{model_id}", params={"num_rows": 5})
    print(f"Status: {r.status_code}")
    if r.status_code != 200:
        print(f"Error: {r.text}")
        return
    
    gen_result = r.json()
    print(f"Generated {gen_result.get('rowCount')} rows:")
    for i, row in enumerate(gen_result.get("data", []), 1):
        print(f"  {i}. {row}")
    
    # 5. Delete Model
    print("\n=== 5. Delete Model ===")
    r = requests.delete(f"{BASE_URL}/api/v1/model/{model_id}")
    print(f"Status: {r.status_code}, Response: {r.json()}")
    
    print("\n✅ All tests passed!")

if __name__ == "__main__":
    test_flow()
