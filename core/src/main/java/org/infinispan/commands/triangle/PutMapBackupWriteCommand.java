package org.infinispan.commands.triangle;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commands.write.PutMapCommand;
import org.infinispan.commands.write.WriteCommand;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.functional.impl.MetaParamsInternalMetadata;
import org.infinispan.metadata.Metadata;
import org.infinispan.util.ByteString;
import org.infinispan.util.TriangleFunctionsUtil;

/**
 * A {@link BackupWriteCommand} implementation for {@link PutMapCommand}.
 *
 * @author Pedro Ruivo
 * @since 9.2
 */
public class PutMapBackupWriteCommand extends BackupWriteCommand {

   public static final byte COMMAND_ID = 78;

   private Map<Object, Object> map;
   private Metadata metadata;
   private Map<Object, MetaParamsInternalMetadata> internalMetadataMap;

   //for testing
   @SuppressWarnings("unused")
   public PutMapBackupWriteCommand() {
      super(null);
   }

   public PutMapBackupWriteCommand(ByteString cacheName) {
      super(cacheName);
   }

   @Override
   public byte getCommandId() {
      return COMMAND_ID;
   }

   @Override
   public void writeTo(ObjectOutput output) throws IOException {
      writeBase(output);
      MarshallUtil.marshallMap(map, output);
      output.writeObject(metadata);
      MarshallUtil.marshallMap(internalMetadataMap, output);
   }

   @Override
   public void readFrom(ObjectInput input) throws IOException, ClassNotFoundException {
      readBase(input);
      map = MarshallUtil.unmarshallMap(input, HashMap::new);
      metadata = (Metadata) input.readObject();
      internalMetadataMap = MarshallUtil.unmarshallMap(input, HashMap::new);
   }

   public void setPutMapCommand(PutMapCommand command, Collection<Object> keys) {
      setCommonAttributesFromCommand(command);
      this.map = TriangleFunctionsUtil.filterEntries(command.getMap(), keys);
      this.metadata = command.getMetadata();
      this.internalMetadataMap = new HashMap<>();
      for (Object key : map.keySet()) {
         internalMetadataMap.put(key, command.getInternalMetadata(key));
      }
   }

   @Override
   public String toString() {
      return "PutMapBackupWriteCommand{" + toStringFields() + '}';
   }

   @Override
   WriteCommand createWriteCommand() {
      PutMapCommand cmd = new PutMapCommand(map, metadata, getFlags(), getCommandInvocationId());
      cmd.setForwarded(true);
      internalMetadataMap.forEach(cmd::setInternalMetadata);
      return cmd;
   }

   @Override
   String toStringFields() {
      return super.toStringFields() +
            ", map=" + map +
            ", metadata=" + metadata +
            ", internalMetadata=" + internalMetadataMap;
   }
}
