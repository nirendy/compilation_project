#file_name='Arrays'
base_dir='examples/ex2'
in_file_path='in'
out_file_path='out'
specific=''
#specific='.test'
out_file_path="out$specific"


rm -r $base_dir/$out_file_path/

mkdir $base_dir/$out_file_path

for path in $(ls $base_dir/$in_file_path/*$specific.xml); do
  file_name=$(basename $path .xml )
	echo "Checking $file_name..."

  java -jar mjavac.jar unmarshal print $base_dir/$in_file_path/$file_name.xml $base_dir/$out_file_path/$file_name.java
  java -jar mjavac.jar unmarshal compile $base_dir/$in_file_path/$file_name.xml $base_dir/$out_file_path/$file_name.ll

  java $base_dir/$out_file_path/$file_name.java > $base_dir/$out_file_path/$file_name.java.out

  clang -o $base_dir/$out_file_path/$file_name.asm $base_dir/$out_file_path/$file_name.ll
  ./$base_dir/$out_file_path/$file_name.asm > $base_dir/$out_file_path/$file_name.ll.out

  diff $base_dir/$out_file_path/$file_name.ll.out $base_dir/$out_file_path/$file_name.java.out
  echo "================================="


done