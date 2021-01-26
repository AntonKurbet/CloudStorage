Cloud Storage
тут 2 варианта передачи:
1. getObjectSequence() - объектами FileMessage - частями по PART_LEN байт данных, 
   через ObjectInputStream/ObjectOutputStream
2. getByProtocol() - через DataInputStream/DataOutputStream в формате протокола
      31<длина имени><имя><длина данных><данные>
   одним куском