import os
import re

directory = r'd:\AndroidStudioProjects\UnDrift\app\src\main\java\com\undrift\ui\screens'

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if 'RoundedCornerShape' in content:
                # Replace the import
                content = content.replace('import androidx.compose.foundation.shape.RoundedCornerShape', 'import com.undrift.ui.components.SquircleShape')
                # Replace the usages
                content = re.sub(r'RoundedCornerShape\([^)]*\)', 'SquircleShape()', content)
                
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"Updated {file}")
