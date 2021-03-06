/**
 * Autogenerated by Thrift Compiler (0.11.0)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.flume.service;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked", "unused"})
@javax.annotation.Generated(value = "Autogenerated by Thrift Compiler (0.11.0)", date = "2018-08-13")
public class ESSink implements org.apache.thrift.TBase<ESSink, ESSink._Fields>, java.io.Serializable, Cloneable, Comparable<ESSink> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("ESSink");

  private static final org.apache.thrift.protocol.TField HOST_NAMES_FIELD_DESC = new org.apache.thrift.protocol.TField("hostNames", org.apache.thrift.protocol.TType.STRING, (short)1);
  private static final org.apache.thrift.protocol.TField INDEX_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("indexName", org.apache.thrift.protocol.TType.STRING, (short)2);
  private static final org.apache.thrift.protocol.TField INDEX_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("indexType", org.apache.thrift.protocol.TType.STRING, (short)3);
  private static final org.apache.thrift.protocol.TField CLUSTER_NAME_FIELD_DESC = new org.apache.thrift.protocol.TField("clusterName", org.apache.thrift.protocol.TType.STRING, (short)4);
  private static final org.apache.thrift.protocol.TField CONTENT_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("contentType", org.apache.thrift.protocol.TType.STRING, (short)5);

  private static final org.apache.thrift.scheme.SchemeFactory STANDARD_SCHEME_FACTORY = new ESSinkStandardSchemeFactory();
  private static final org.apache.thrift.scheme.SchemeFactory TUPLE_SCHEME_FACTORY = new ESSinkTupleSchemeFactory();

  public java.lang.String hostNames; // required
  public java.lang.String indexName; // optional
  public java.lang.String indexType; // optional
  public java.lang.String clusterName; // optional
  public java.lang.String contentType; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    HOST_NAMES((short)1, "hostNames"),
    INDEX_NAME((short)2, "indexName"),
    INDEX_TYPE((short)3, "indexType"),
    CLUSTER_NAME((short)4, "clusterName"),
    CONTENT_TYPE((short)5, "contentType");

    private static final java.util.Map<java.lang.String, _Fields> byName = new java.util.HashMap<java.lang.String, _Fields>();

    static {
      for (_Fields field : java.util.EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // HOST_NAMES
          return HOST_NAMES;
        case 2: // INDEX_NAME
          return INDEX_NAME;
        case 3: // INDEX_TYPE
          return INDEX_TYPE;
        case 4: // CLUSTER_NAME
          return CLUSTER_NAME;
        case 5: // CONTENT_TYPE
          return CONTENT_TYPE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new java.lang.IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(java.lang.String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final java.lang.String _fieldName;

    _Fields(short thriftId, java.lang.String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public java.lang.String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  private static final _Fields optionals[] = {_Fields.INDEX_NAME,_Fields.INDEX_TYPE,_Fields.CLUSTER_NAME,_Fields.CONTENT_TYPE};
  public static final java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    java.util.Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new java.util.EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.HOST_NAMES, new org.apache.thrift.meta_data.FieldMetaData("hostNames", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.INDEX_NAME, new org.apache.thrift.meta_data.FieldMetaData("indexName", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.INDEX_TYPE, new org.apache.thrift.meta_data.FieldMetaData("indexType", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.CLUSTER_NAME, new org.apache.thrift.meta_data.FieldMetaData("clusterName", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    tmpMap.put(_Fields.CONTENT_TYPE, new org.apache.thrift.meta_data.FieldMetaData("contentType", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.STRING)));
    metaDataMap = java.util.Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(ESSink.class, metaDataMap);
  }

  public ESSink() {
  }

  public ESSink(
    java.lang.String hostNames)
  {
    this();
    this.hostNames = hostNames;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public ESSink(ESSink other) {
    if (other.isSetHostNames()) {
      this.hostNames = other.hostNames;
    }
    if (other.isSetIndexName()) {
      this.indexName = other.indexName;
    }
    if (other.isSetIndexType()) {
      this.indexType = other.indexType;
    }
    if (other.isSetClusterName()) {
      this.clusterName = other.clusterName;
    }
    if (other.isSetContentType()) {
      this.contentType = other.contentType;
    }
  }

  public ESSink deepCopy() {
    return new ESSink(this);
  }

  @Override
  public void clear() {
    this.hostNames = null;
    this.indexName = null;
    this.indexType = null;
    this.clusterName = null;
    this.contentType = null;
  }

  public java.lang.String getHostNames() {
    return this.hostNames;
  }

  public ESSink setHostNames(java.lang.String hostNames) {
    this.hostNames = hostNames;
    return this;
  }

  public void unsetHostNames() {
    this.hostNames = null;
  }

  /** Returns true if field hostNames is set (has been assigned a value) and false otherwise */
  public boolean isSetHostNames() {
    return this.hostNames != null;
  }

  public void setHostNamesIsSet(boolean value) {
    if (!value) {
      this.hostNames = null;
    }
  }

  public java.lang.String getIndexName() {
    return this.indexName;
  }

  public ESSink setIndexName(java.lang.String indexName) {
    this.indexName = indexName;
    return this;
  }

  public void unsetIndexName() {
    this.indexName = null;
  }

  /** Returns true if field indexName is set (has been assigned a value) and false otherwise */
  public boolean isSetIndexName() {
    return this.indexName != null;
  }

  public void setIndexNameIsSet(boolean value) {
    if (!value) {
      this.indexName = null;
    }
  }

  public java.lang.String getIndexType() {
    return this.indexType;
  }

  public ESSink setIndexType(java.lang.String indexType) {
    this.indexType = indexType;
    return this;
  }

  public void unsetIndexType() {
    this.indexType = null;
  }

  /** Returns true if field indexType is set (has been assigned a value) and false otherwise */
  public boolean isSetIndexType() {
    return this.indexType != null;
  }

  public void setIndexTypeIsSet(boolean value) {
    if (!value) {
      this.indexType = null;
    }
  }

  public java.lang.String getClusterName() {
    return this.clusterName;
  }

  public ESSink setClusterName(java.lang.String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  public void unsetClusterName() {
    this.clusterName = null;
  }

  /** Returns true if field clusterName is set (has been assigned a value) and false otherwise */
  public boolean isSetClusterName() {
    return this.clusterName != null;
  }

  public void setClusterNameIsSet(boolean value) {
    if (!value) {
      this.clusterName = null;
    }
  }

  public java.lang.String getContentType() {
    return this.contentType;
  }

  public ESSink setContentType(java.lang.String contentType) {
    this.contentType = contentType;
    return this;
  }

  public void unsetContentType() {
    this.contentType = null;
  }

  /** Returns true if field contentType is set (has been assigned a value) and false otherwise */
  public boolean isSetContentType() {
    return this.contentType != null;
  }

  public void setContentTypeIsSet(boolean value) {
    if (!value) {
      this.contentType = null;
    }
  }

  public void setFieldValue(_Fields field, java.lang.Object value) {
    switch (field) {
    case HOST_NAMES:
      if (value == null) {
        unsetHostNames();
      } else {
        setHostNames((java.lang.String)value);
      }
      break;

    case INDEX_NAME:
      if (value == null) {
        unsetIndexName();
      } else {
        setIndexName((java.lang.String)value);
      }
      break;

    case INDEX_TYPE:
      if (value == null) {
        unsetIndexType();
      } else {
        setIndexType((java.lang.String)value);
      }
      break;

    case CLUSTER_NAME:
      if (value == null) {
        unsetClusterName();
      } else {
        setClusterName((java.lang.String)value);
      }
      break;

    case CONTENT_TYPE:
      if (value == null) {
        unsetContentType();
      } else {
        setContentType((java.lang.String)value);
      }
      break;

    }
  }

  public java.lang.Object getFieldValue(_Fields field) {
    switch (field) {
    case HOST_NAMES:
      return getHostNames();

    case INDEX_NAME:
      return getIndexName();

    case INDEX_TYPE:
      return getIndexType();

    case CLUSTER_NAME:
      return getClusterName();

    case CONTENT_TYPE:
      return getContentType();

    }
    throw new java.lang.IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new java.lang.IllegalArgumentException();
    }

    switch (field) {
    case HOST_NAMES:
      return isSetHostNames();
    case INDEX_NAME:
      return isSetIndexName();
    case INDEX_TYPE:
      return isSetIndexType();
    case CLUSTER_NAME:
      return isSetClusterName();
    case CONTENT_TYPE:
      return isSetContentType();
    }
    throw new java.lang.IllegalStateException();
  }

  @Override
  public boolean equals(java.lang.Object that) {
    if (that == null)
      return false;
    if (that instanceof ESSink)
      return this.equals((ESSink)that);
    return false;
  }

  public boolean equals(ESSink that) {
    if (that == null)
      return false;
    if (this == that)
      return true;

    boolean this_present_hostNames = true && this.isSetHostNames();
    boolean that_present_hostNames = true && that.isSetHostNames();
    if (this_present_hostNames || that_present_hostNames) {
      if (!(this_present_hostNames && that_present_hostNames))
        return false;
      if (!this.hostNames.equals(that.hostNames))
        return false;
    }

    boolean this_present_indexName = true && this.isSetIndexName();
    boolean that_present_indexName = true && that.isSetIndexName();
    if (this_present_indexName || that_present_indexName) {
      if (!(this_present_indexName && that_present_indexName))
        return false;
      if (!this.indexName.equals(that.indexName))
        return false;
    }

    boolean this_present_indexType = true && this.isSetIndexType();
    boolean that_present_indexType = true && that.isSetIndexType();
    if (this_present_indexType || that_present_indexType) {
      if (!(this_present_indexType && that_present_indexType))
        return false;
      if (!this.indexType.equals(that.indexType))
        return false;
    }

    boolean this_present_clusterName = true && this.isSetClusterName();
    boolean that_present_clusterName = true && that.isSetClusterName();
    if (this_present_clusterName || that_present_clusterName) {
      if (!(this_present_clusterName && that_present_clusterName))
        return false;
      if (!this.clusterName.equals(that.clusterName))
        return false;
    }

    boolean this_present_contentType = true && this.isSetContentType();
    boolean that_present_contentType = true && that.isSetContentType();
    if (this_present_contentType || that_present_contentType) {
      if (!(this_present_contentType && that_present_contentType))
        return false;
      if (!this.contentType.equals(that.contentType))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 1;

    hashCode = hashCode * 8191 + ((isSetHostNames()) ? 131071 : 524287);
    if (isSetHostNames())
      hashCode = hashCode * 8191 + hostNames.hashCode();

    hashCode = hashCode * 8191 + ((isSetIndexName()) ? 131071 : 524287);
    if (isSetIndexName())
      hashCode = hashCode * 8191 + indexName.hashCode();

    hashCode = hashCode * 8191 + ((isSetIndexType()) ? 131071 : 524287);
    if (isSetIndexType())
      hashCode = hashCode * 8191 + indexType.hashCode();

    hashCode = hashCode * 8191 + ((isSetClusterName()) ? 131071 : 524287);
    if (isSetClusterName())
      hashCode = hashCode * 8191 + clusterName.hashCode();

    hashCode = hashCode * 8191 + ((isSetContentType()) ? 131071 : 524287);
    if (isSetContentType())
      hashCode = hashCode * 8191 + contentType.hashCode();

    return hashCode;
  }

  @Override
  public int compareTo(ESSink other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = java.lang.Boolean.valueOf(isSetHostNames()).compareTo(other.isSetHostNames());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetHostNames()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.hostNames, other.hostNames);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetIndexName()).compareTo(other.isSetIndexName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndexName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.indexName, other.indexName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetIndexType()).compareTo(other.isSetIndexType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetIndexType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.indexType, other.indexType);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetClusterName()).compareTo(other.isSetClusterName());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetClusterName()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.clusterName, other.clusterName);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = java.lang.Boolean.valueOf(isSetContentType()).compareTo(other.isSetContentType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetContentType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.contentType, other.contentType);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    scheme(iprot).read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    scheme(oprot).write(oprot, this);
  }

  @Override
  public java.lang.String toString() {
    java.lang.StringBuilder sb = new java.lang.StringBuilder("ESSink(");
    boolean first = true;

    sb.append("hostNames:");
    if (this.hostNames == null) {
      sb.append("null");
    } else {
      sb.append(this.hostNames);
    }
    first = false;
    if (isSetIndexName()) {
      if (!first) sb.append(", ");
      sb.append("indexName:");
      if (this.indexName == null) {
        sb.append("null");
      } else {
        sb.append(this.indexName);
      }
      first = false;
    }
    if (isSetIndexType()) {
      if (!first) sb.append(", ");
      sb.append("indexType:");
      if (this.indexType == null) {
        sb.append("null");
      } else {
        sb.append(this.indexType);
      }
      first = false;
    }
    if (isSetClusterName()) {
      if (!first) sb.append(", ");
      sb.append("clusterName:");
      if (this.clusterName == null) {
        sb.append("null");
      } else {
        sb.append(this.clusterName);
      }
      first = false;
    }
    if (isSetContentType()) {
      if (!first) sb.append(", ");
      sb.append("contentType:");
      if (this.contentType == null) {
        sb.append("null");
      } else {
        sb.append(this.contentType);
      }
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (hostNames == null) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'hostNames' was not present! Struct: " + toString());
    }
    // check for sub-struct validity
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, java.lang.ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class ESSinkStandardSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ESSinkStandardScheme getScheme() {
      return new ESSinkStandardScheme();
    }
  }

  private static class ESSinkStandardScheme extends org.apache.thrift.scheme.StandardScheme<ESSink> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, ESSink struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // HOST_NAMES
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.hostNames = iprot.readString();
              struct.setHostNamesIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 2: // INDEX_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.indexName = iprot.readString();
              struct.setIndexNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // INDEX_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.indexType = iprot.readString();
              struct.setIndexTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // CLUSTER_NAME
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.clusterName = iprot.readString();
              struct.setClusterNameIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 5: // CONTENT_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRING) {
              struct.contentType = iprot.readString();
              struct.setContentTypeIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();

      // check for required fields of primitive type, which can't be checked in the validate method
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, ESSink struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.hostNames != null) {
        oprot.writeFieldBegin(HOST_NAMES_FIELD_DESC);
        oprot.writeString(struct.hostNames);
        oprot.writeFieldEnd();
      }
      if (struct.indexName != null) {
        if (struct.isSetIndexName()) {
          oprot.writeFieldBegin(INDEX_NAME_FIELD_DESC);
          oprot.writeString(struct.indexName);
          oprot.writeFieldEnd();
        }
      }
      if (struct.indexType != null) {
        if (struct.isSetIndexType()) {
          oprot.writeFieldBegin(INDEX_TYPE_FIELD_DESC);
          oprot.writeString(struct.indexType);
          oprot.writeFieldEnd();
        }
      }
      if (struct.clusterName != null) {
        if (struct.isSetClusterName()) {
          oprot.writeFieldBegin(CLUSTER_NAME_FIELD_DESC);
          oprot.writeString(struct.clusterName);
          oprot.writeFieldEnd();
        }
      }
      if (struct.contentType != null) {
        if (struct.isSetContentType()) {
          oprot.writeFieldBegin(CONTENT_TYPE_FIELD_DESC);
          oprot.writeString(struct.contentType);
          oprot.writeFieldEnd();
        }
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class ESSinkTupleSchemeFactory implements org.apache.thrift.scheme.SchemeFactory {
    public ESSinkTupleScheme getScheme() {
      return new ESSinkTupleScheme();
    }
  }

  private static class ESSinkTupleScheme extends org.apache.thrift.scheme.TupleScheme<ESSink> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, ESSink struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol oprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      oprot.writeString(struct.hostNames);
      java.util.BitSet optionals = new java.util.BitSet();
      if (struct.isSetIndexName()) {
        optionals.set(0);
      }
      if (struct.isSetIndexType()) {
        optionals.set(1);
      }
      if (struct.isSetClusterName()) {
        optionals.set(2);
      }
      if (struct.isSetContentType()) {
        optionals.set(3);
      }
      oprot.writeBitSet(optionals, 4);
      if (struct.isSetIndexName()) {
        oprot.writeString(struct.indexName);
      }
      if (struct.isSetIndexType()) {
        oprot.writeString(struct.indexType);
      }
      if (struct.isSetClusterName()) {
        oprot.writeString(struct.clusterName);
      }
      if (struct.isSetContentType()) {
        oprot.writeString(struct.contentType);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, ESSink struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TTupleProtocol iprot = (org.apache.thrift.protocol.TTupleProtocol) prot;
      struct.hostNames = iprot.readString();
      struct.setHostNamesIsSet(true);
      java.util.BitSet incoming = iprot.readBitSet(4);
      if (incoming.get(0)) {
        struct.indexName = iprot.readString();
        struct.setIndexNameIsSet(true);
      }
      if (incoming.get(1)) {
        struct.indexType = iprot.readString();
        struct.setIndexTypeIsSet(true);
      }
      if (incoming.get(2)) {
        struct.clusterName = iprot.readString();
        struct.setClusterNameIsSet(true);
      }
      if (incoming.get(3)) {
        struct.contentType = iprot.readString();
        struct.setContentTypeIsSet(true);
      }
    }
  }

  private static <S extends org.apache.thrift.scheme.IScheme> S scheme(org.apache.thrift.protocol.TProtocol proto) {
    return (org.apache.thrift.scheme.StandardScheme.class.equals(proto.getScheme()) ? STANDARD_SCHEME_FACTORY : TUPLE_SCHEME_FACTORY).getScheme();
  }
}

