#!/usr/bin/env python3
"""
Script to remove ActorContext from Java service and controller files.
"""
import os
import re
import sys

def remove_actor_context_from_service(file_path):
    """Remove ActorContext from a service file."""
    with open(file_path, 'r') as f:
        content = f.read()
    
    original = content
    
    # Remove ActorContext import
    content = re.sub(r'^import com\.mytegroup\.api\.service\.common\.ActorContext;\s*\n', '', content, flags=re.MULTILINE)
    
    # Remove ActorContext parameter from method signatures
    content = re.sub(r',\s*ActorContext\s+actor\s*', '', content)
    content = re.sub(r'ActorContext\s+actor\s*,', '', content)
    content = re.sub(r'\(\s*ActorContext\s+actor\s*\)', '()', content)
    content = re.sub(r'\(\s*ActorContext\s+actor\s*,', '(', content)
    
    # Remove authHelper calls with actor
    content = re.sub(r'authHelper\.ensureRole\s*\(\s*actor\s*,.*?\);\s*\n', '', content, flags=re.MULTILINE | re.DOTALL)
    content = re.sub(r'authHelper\.ensureOrgScope\s*\([^,]+,\s*actor\s*\);\s*\n', '', content, flags=re.MULTILINE)
    content = re.sub(r'authHelper\.canViewArchived\s*\(\s*actor\s*\)', 'false', content)
    content = re.sub(r'authHelper\.resolveOrgId\s*\([^,]+,\s*actor\s*\)', r'\1', content)
    
    # Replace actor.getUserId() with null
    content = re.sub(r'actor\s*!=\s*null\s*\?\s*actor\.getUserId\s*\(\s*\)\s*:\s*null', 'null', content)
    content = re.sub(r'actor\.getUserId\s*\(\s*\)', 'null', content)
    content = re.sub(r'actor\.getOrgId\s*\(\s*\)', 'null', content)
    content = re.sub(r'actor\.getRole\s*\(\s*\)', 'null', content)
    
    # Add orgId validation if missing
    if 'public' in content and 'String orgId' in content:
        # Add basic validation for methods that take orgId
        pass  # Will be handled manually for complex cases
    
    if content != original:
        with open(file_path, 'w') as f:
            f.write(content)
        return True
    return False

def remove_actor_context_from_controller(file_path):
    """Remove ActorContext from a controller file."""
    with open(file_path, 'r') as f:
        content = f.read()
    
    original = content
    
    # Remove ActorContext import
    content = re.sub(r'^import com\.mytegroup\.api\.service\.common\.ActorContext;\s*\n', '', content, flags=re.MULTILINE)
    
    # Remove SecurityContextHolder and Authentication imports if no longer needed
    if 'getActorContext' not in content:
        content = re.sub(r'^import org\.springframework\.security\.core\.Authentication;\s*\n', '', content, flags=re.MULTILINE)
        content = re.sub(r'^import org\.springframework\.security\.core\.context\.SecurityContextHolder;\s*\n', '', content, flags=re.MULTILINE)
    
    # Remove getActorContext method (basic pattern)
    content = re.sub(r'private\s+ActorContext\s+getActorContext\s*\([^)]*\)\s*\{[^}]*\}[^}]*\}[^}]*\}', '', content, flags=re.MULTILINE | re.DOTALL)
    
    # Remove ActorContext variable declarations and usage
    content = re.sub(r'ActorContext\s+actor\s*=\s*getActorContext\s*\([^)]*\);\s*\n', '', content, flags=re.MULTILINE)
    content = re.sub(r'String\s+resolvedOrgId\s*=\s*orgId\s*!=\s*null\s*\?\s*orgId\s*:\s*actor\.getOrgId\s*\(\s*\);\s*\n', 
                     'if (orgId == null) { return ResponseEntity.badRequest().body(Map.of("error", "orgId is required")); }\n', 
                     content, flags=re.MULTILINE)
    
    # Replace service calls that use actor
    content = re.sub(r'(\w+Service\.\w+\s*\([^,)]+),\s*actor\s*,', r'\1,', content)
    content = re.sub(r'(\w+Service\.\w+\s*\(),\s*actor\s*,', r'\1', content)
    content = re.sub(r',\s*actor\s*\)', ')', content)
    content = re.sub(r'\(\s*actor\s*,', '(', content)
    
    if content != original:
        with open(file_path, 'w') as f:
            f.write(content)
        return True
    return False

def main():
    base_dir = 'src/main/java'
    
    # Process services
    service_files = []
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith('Service.java'):
                file_path = os.path.join(root, file)
                if 'ActorContext' in open(file_path).read():
                    service_files.append(file_path)
    
    print(f"Processing {len(service_files)} service files...")
    for file_path in service_files:
        try:
            if remove_actor_context_from_service(file_path):
                print(f"  Updated: {file_path}")
        except Exception as e:
            print(f"  Error processing {file_path}: {e}")
    
    # Process controllers
    controller_files = []
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith('Controller.java'):
                file_path = os.path.join(root, file)
                if 'ActorContext' in open(file_path).read():
                    controller_files.append(file_path)
    
    print(f"\nProcessing {len(controller_files)} controller files...")
    for file_path in controller_files:
        try:
            if remove_actor_context_from_controller(file_path):
                print(f"  Updated: {file_path}")
        except Exception as e:
            print(f"  Error processing {file_path}: {e}")
    
    print("\nDone! Please review the changes and fix any edge cases manually.")

if __name__ == '__main__':
    main()

