"""安装可视化依赖包"""
import subprocess
import sys

def install_package(package):
    """安装Python包"""
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", package])
        print(f"✅ {package} 安装成功")
        return True
    except subprocess.CalledProcessError:
        print(f"❌ {package} 安装失败")
        return False

def main():
    """安装所需的可视化依赖"""
    print("开始安装可视化依赖包...")
    
    packages = [
        "matplotlib",
        "seaborn", 
        "pandas",
        "numpy",
        "requests"
    ]
    
    success_count = 0
    for package in packages:
        if install_package(package):
            success_count += 1
    
    print(f"\n安装完成: {success_count}/{len(packages)} 个包安装成功")
    
    if success_count == len(packages):
        print("\n✅ 所有依赖安装成功！现在可以运行可视化脚本了:")
        print("python generate_visualization.py  # 直接生成图表")
        print("python api_visualization.py      # 通过API生成图表")
    else:
        print("\n❌ 部分依赖安装失败，请手动安装:")
        for package in packages:
            print(f"pip install {package}")

if __name__ == "__main__":
    main()